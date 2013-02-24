/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.arquillian.gwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitHost.ClientInfo;
import com.google.gwt.junit.client.impl.JUnitHost.TestBlock;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A message queue to pass data between {@link JUnitShell} and {@link org.jboss.arquillian.gwt.ArquillianJunitHostImpl}
 * in a thread-safe manner.
 * 
 * <p>
 * The public methods are called by the servlet to find out what test to execute next, and to report the results of the
 * last test to run.
 * </p>
 * 
 * <p>
 * The protected methods are called by the shell to fetch test results and drive the next test the client should run.
 * </p>
 */
public class ArquillianJunitMessageQueue implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Server-side client info that includes a description.
   */
  public static class ClientInfoExt extends ClientInfo implements Serializable, IsSerializable {
    private static final long serialVersionUID = 1L;

    /**
     * A description of this client.
     */
    private final String desc;

    public ClientInfoExt(int sessionId, String userAgent, String desc) {
      super(sessionId, userAgent);
      this.desc = desc;
    }

    public String getDesc() {
      return desc;
    }
  }

  /**
   * Holds the state of an individual client.
   */
  public static class ClientStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    private int blockIndex = 0;
    private ClientInfoExt clientInfo;
    private boolean isNew = true;

    public ClientStatus(ClientInfoExt clientInfo) {
      this.clientInfo = clientInfo;
    }

    public String getDesc() {
      return clientInfo.getDesc();
    }

    public int getId() {
      return clientInfo.getSessionId();
    }

    @Override
    public String toString() {
      return clientInfo.getDesc();
    }

    public void updateClientInfo(ClientInfoExt clientInfo) {
      this.clientInfo = clientInfo;
    }
  }

  private static final Set<Class<? extends Throwable>> THROWABLES_NOT_RETRIED = createThrowablesNotRetried();

  private static Set<Class<? extends Throwable>> createThrowablesNotRetried() {
    Set<Class<? extends Throwable>> throwableSet = new HashSet<Class<? extends Throwable>>();
    throwableSet.add(com.google.gwt.junit.JUnitFatalLaunchException.class);
    throwableSet.add(java.lang.Error.class);
    return throwableSet;
  }

  /**
   * Records results for each client; must lock before accessing.
   */
  private final Map<Integer, ClientStatus> clientStatuses = new HashMap<Integer, ClientStatus>();

  /**
   * The lock used to synchronize access to clientStatuses.
   */
  private final transient Object clientStatusesLock = new Object();

  /**
   * Set to true when the last test block has been added. This is used to tell clients that all tests are complete.
   */
  private boolean isLastTestBlockAvailable;

  /**
   * The number of TestCase clients executing in parallel.
   */
  private int numClients = 1;

  /**
   * The list of test blocks to run.
   */
  private final List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();

  /**
   * Maps the TestInfo to the results from each clientId. If JUnitResult is null, it means that the client requested the
   * test but did not report the results yet.
   */
  private final Map<TestInfo, Map<ClientStatus, JUnitResult>> testResults =
      new HashMap<TestInfo, Map<ClientStatus, JUnitResult>>();

  /**
   * A set of the GWT user agents (eg. ie6, gecko1_8) that have connected.
   */
  private final Set<String> userAgents = new HashSet<String>();

  public ArquillianJunitMessageQueue() {

  }

  public ArquillianJunitMessageQueue(int numClients) {
    this.numClients = numClients;
  }

  /**
   * Called by the servlet to query for for the next block to test.
   * 
   * @param clientInfo
   *          information about the client
   * @param blockIndex
   *          the index of the test block to get
   * @param timeout
   *          how long to wait for an answer
   * @return the next test to run, or <code>null</code> if <code>timeout</code> is exceeded or the next test does not
   *         match <code>testClassName</code>
   */
  public TestBlock getTestBlock(ClientInfoExt clientInfo, int blockIndex,
      long timeout) throws TimeoutException {
    userAgents.add(clientInfo.getUserAgent());
    ClientStatus clientStatus = ensureClientStatus(clientInfo);
    clientStatus.blockIndex = blockIndex;

    // The client has finished all of the tests.
    if (isLastTestBlockAvailable && blockIndex >= testBlocks.size()) {
      return null;
    }

    // Record that this client has retrieved the current tests.
    if (blockIndex < testBlocks.size()) {
      TestInfo[] tests = testBlocks.get(blockIndex);
      for (TestInfo testInfo : tests) {
        ensureResults(testInfo).put(clientStatus, null);
      }
      return new TestBlock(tests, blockIndex);
    }
    return null;

  }

  /**
   * Reports a failure from a client that cannot startup.
   * 
   * @param clientInfo
   *          information about the client
   * @param result
   *          the failure result
   */
  public void reportFatalLaunch(ClientInfoExt clientInfo, JUnitResult result) {
    // Fatal launch error, cause this client to fail the whole block.
    ClientStatus clientStatus = ensureClientStatus(clientInfo);
    Map<TestInfo, JUnitResult> results = new HashMap<TestInfo, JUnitResult>();
    for (TestInfo testInfo : testBlocks.get(clientStatus.blockIndex)) {
      results.put(testInfo, result);
    }
    reportResults(clientInfo, results);
  }

  /**
   * Called by the servlet to report the results of the last test to run.
   * 
   * @param clientInfo
   *          information about the client
   * @param results
   *          the result of running the test block
   */
  public void reportResults(ClientInfoExt clientInfo,
      Map<TestInfo, JUnitResult> results) {
    if (results == null) {
      throw new IllegalArgumentException("results cannot be null");
    }
    userAgents.add(clientInfo.getUserAgent());
    ClientStatus clientStatus = ensureClientStatus(clientInfo);

    // Cache the test results.
    for (Map.Entry<TestInfo, JUnitResult> entry : results.entrySet()) {
      TestInfo testInfo = entry.getKey();
      ensureResults(testInfo).put(clientStatus, entry.getValue());
    }

    writeToFile();
  }

  /**
   * Called by the shell to add test blocks to test.
   * 
   * @param isLastBlock
   *          true if this is the last test block that will be added
   */
  public void addTestBlocks(List<TestInfo[]> newTestBlocks, boolean isLastBlock) {
    if (isLastTestBlockAvailable) {
      throw new IllegalArgumentException(
            "Cannot add test blocks after the last block is added");
    }
    for (TestInfo[] testBlock : newTestBlocks) {
      if (testBlock.length == 0) {
        throw new IllegalArgumentException("TestBlocks cannot be empty");
      }
    }
    testBlocks.addAll(newTestBlocks);
    if (isLastBlock) {
      isLastTestBlockAvailable = true;
    }
    writeToFile();
  }

  /**
   * Returns any new clients that have contacted the server since the last call. The same client will never be returned
   * from this method twice.
   */
  public String[] getNewClients() {
    List<String> results = new ArrayList<String>();
    for (ClientStatus clientStatus : clientStatuses.values()) {
      if (clientStatus.isNew) {
        results.add(clientStatus.getDesc());
        // Record that this client is no longer new.
        clientStatus.isNew = false;
      }
    }
    return results.toArray(new String[results.size()]);
  }

  public int getNumClients() {
    return numClients;
  }

  /**
   * Returns how many clients have requested the currently-running test.
   * 
   * @param testInfo
   *          the {@link TestInfo} that the clients retrieved
   */
  public int getNumClientsRetrievedTest(TestInfo testInfo) {
    int count = 0;
    Map<ClientStatus, JUnitResult> results = testResults.get(testInfo);
    if (results != null) {
      count = results.size();
    }
    return count;
  }

  /**
   * Returns how many clients have connected.
   */
  public int getNumConnectedClients() {
    return clientStatuses.size();
  }

  /**
   * Fetches the results of a completed test.
   * 
   * @param testInfo
   *          the {@link TestInfo} to check for results
   * @return A map of results from all clients.
   */
  public Map<ClientStatus, JUnitResult> getResults(TestInfo testInfo) {
    return testResults.get(testInfo);
  }

  /**
   * Visible for testing.
   * 
   * @return the test blocks
   */
  public List<TestInfo[]> getTestBlocks() {
    return testBlocks;
  }

  /**
   * Returns a pretty printed list of clients that have not retrieved the current test. Used for error reporting.
   * 
   * @param testInfo
   *          the {@link TestInfo} we are waiting for
   * @return a string containing the list of clients that have not retrieved the current test.
   */
  public String getUnretrievedClients(TestInfo testInfo) {
    Map<ClientStatus, JUnitResult> results = testResults.get(testInfo);
    StringBuilder buf = new StringBuilder();
    int lineCount = 0;
    for (ClientStatus clientStatus : clientStatuses.values()) {
      if (lineCount > 0) {
        buf.append('\n');
      }

      if (results == null || !results.containsKey(clientStatus)) {
        buf.append(" - NO RESPONSE: ");
      }
      else {
        buf.append(" - (ok): ");
      }
      buf.append(clientStatus.getDesc());
      ++lineCount;
    }
    int difference = numClients - getNumClientsRetrievedTest(testInfo);
    if (difference > 0) {
      if (lineCount > 0) {
        buf.append('\n');
      }
      buf.append(" - "
            + difference
            + " client(s) haven't responded back to JUnitShell since the start of the test.");
    }
    return buf.toString();
  }

  /**
   * Returns a list of all user agents that have connected.
   */
  public String[] getUserAgents() {
    // synchronized (clientStatusesLock) {
    return userAgents.toArray(new String[userAgents.size()]);
    // }
  }

  /**
   * Returns a human-formatted message identifying what clients have connected but have not yet reported results for
   * this test. It is used in a timeout condition, to identify what we're still waiting on.
   * 
   * @param testInfo
   *          the {@link TestInfo} that the clients are working on
   * @return human readable message
   */
  public String getWorkingClients(TestInfo testInfo) {
    // Print a list of clients that have connected but not returned results.
    int itemCount = 0;
    StringBuilder buf = new StringBuilder();
    Map<ClientStatus, JUnitResult> results = testResults.get(testInfo);
    if (results != null) {
      for (Map.Entry<ClientStatus, JUnitResult> entry : results.entrySet()) {
        if (entry.getValue() == null) {
          buf.append(entry.getKey().getDesc());
          buf.append("\n");
          itemCount++;
        }
      }
    }

    // Print the number of other clients.
    int difference = numClients - itemCount;
    if (difference > 0) {
      if (itemCount > 0) {
        buf.append('\n');
      }
      buf.append(difference
            + " other client(s) haven't responded back to JUnitShell since the start of the test.");
    }
    return buf.toString();
  }

  /**
   * Called by the shell to see if the currently-running test has completed.
   * 
   * @param testInfo
   *          the {@link TestInfo} to check for results
   * @return If the test has completed, <code>true</code>, otherwise <code>false</code>.
   */
  public boolean hasResults(TestInfo testInfo) {
    Map<ClientStatus, JUnitResult> results = testResults.get(testInfo);
    if (results == null || results.size() < numClients) {
      return false;
    }
    for (JUnitResult result : results.values()) {
      if (result == null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff any there are no results, missing results, or any of the test results is an exception other than
   * those in {@code THROWABLES_NOT_RETRIED}.
   */
  public boolean needsRerunning(TestInfo testInfo) {
    Map<ClientStatus, JUnitResult> results = getResults(testInfo);
    if (results == null) {
      return true;
    }
    if (results.size() != numClients) {
      return true;
    }
    for (Entry<ClientStatus, JUnitResult> entry : results.entrySet()) {
      JUnitResult result = entry.getValue();
      if (result == null) {
        return true;
      }
      Throwable exception = result.getException();
      if (exception != null && !isMember(exception, THROWABLES_NOT_RETRIED)) {
        return true;
      }
    }
    return false;
  }

  public void removeResults(TestInfo testInfo) {
    testResults.remove(testInfo);
  }

  public void waitForResults(int millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
    }
  }

  /**
   * Ensure that a {@link ClientStatus} for the clientId exists.
   * 
   * @param clientId
   *          the id of the client
   * @return the {@link ClientStatus} for the client
   */
  private ClientStatus ensureClientStatus(ClientInfoExt clientInfo) {
    int id = clientInfo.getSessionId();
    ClientStatus clientStatus = clientStatuses.get(id);
    if (clientStatus == null) {
      clientStatus = new ClientStatus(clientInfo);
      clientStatuses.put(id, clientStatus);
    }
    else {
      // Maybe update the client info (IP might change if through a proxy).
      clientStatus.updateClientInfo(clientInfo);
    }
    writeToFile();
    return clientStatus;
  }

  /**
   * Get the map of test results from all clients for a given {@link TestInfo}, creating it if necessary.
   * 
   * @param testInfo
   *          the {@link TestInfo}
   * @return the map of all results
   */
  private Map<ClientStatus, JUnitResult> ensureResults(TestInfo testInfo) {
    Map<ClientStatus, JUnitResult> results = testResults.get(testInfo);
    if (results == null) {
      results = new IdentityHashMap<ClientStatus, JUnitResult>();
      testResults.put(testInfo, results);
    }
    writeToFile();
    return results;
  }

  private boolean isMember(Throwable exception,
      Set<Class<? extends Throwable>> throwableSet) {
    for (Class<? extends Throwable> throwable : throwableSet) {
      if (throwable.isInstance(exception)) {
        return true;
      }
    }
    return false;
  }

  public void writeToFile() {
    ObjectOutputStream out = null;
    try {
      String tempDir = System.getProperty("java.io.tmpdir");
      File tmpFile = new File(tempDir + File.separator + "gwt-unit-test.queue");
      tmpFile.delete();
      out = new ObjectOutputStream(new FileOutputStream(tmpFile));
      out.writeObject(this);
      out.flush();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      if (out != null) {
        try {
          out.close();
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }      
      }
    }
  }

  public static ArquillianJunitMessageQueue readFromFile() {
    try {
      String tempDir = System.getProperty("java.io.tmpdir");
      ArquillianJunitMessageQueue queue =
          (ArquillianJunitMessageQueue) new ObjectInputStream(new FileInputStream(tempDir + File.separator
              + "gwt-unit-test.queue"))
              .readObject();

      return queue;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

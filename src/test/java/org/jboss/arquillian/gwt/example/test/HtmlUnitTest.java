package org.jboss.arquillian.gwt.example.test;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Arquillian with HTMLUnit support
 * @author kpiwko
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HtmlUnitTest {

	@Deployment
	public static WebArchive sample() {
		return Deployments.sample();
	}
	
	@Test
	public void testBundleAndCodeSplit(@ArquillianResource URL baseUrl)
			throws Exception {

		final WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);

		HtmlPage page = webClient.getPage(baseUrl + "/");
		page.getWebClient().waitForBackgroundJavaScriptStartingBefore(4000);

		HtmlButton codeSplit = (HtmlButton) page.getElementById("splitButton");
		Assert.assertNotNull("There is a button with splitButton id", codeSplit);
		HtmlPage newPage = codeSplit.click();
		newPage.getWebClient().waitForBackgroundJavaScriptStartingBefore(4000);

		HtmlElement response = newPage.getElementById("serverResponse");
		Assert.assertNotNull("There is a response", response);

		String xml = response.asXml();

		Assert.assertTrue("Response contains 'It works!' text",
				xml.contains("It works!"));
		Assert.assertTrue("The response contains <img> tag",
				xml.contains("img"));

		webClient.closeAllWindows();
	}

}

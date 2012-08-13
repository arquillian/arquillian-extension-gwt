package org.jboss.arquillian.gwt;

import java.io.File;
import java.util.Random;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.ModuleHandle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.shell.ArtifactAcceptor;
import com.google.gwt.dev.shell.BrowserChannelServer;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.shell.ModuleSpaceHost;
import com.google.gwt.dev.shell.ShellModuleSpaceHost;

public class ArquillianBrowserWidgetHost implements BrowserWidgetHost {

    private static final Random RNG = new Random();

    // FIXME GENDIR MUST BE SET
    private File genDir = new File(".");

    // FIXME THIS SHOULD GO TO ARQUILLIAN
    private JJSOptions jjsoptions = new JJSOptionsImpl();

    @Override
    public ModuleHandle createModuleLogger(String moduleName, String userAgent, String url, String tabKey, String sessionKey,
            BrowserChannelServer serverChannel, byte[] userAgentIcon) {

        if (sessionKey == null) {
            // if we don't have a unique session key, make one up
            sessionKey = randomString();
        }
        // TODO: GWT is able to have remote handles here as well
        return getModuleHandle();
    }

    @Override
    public ModuleSpaceHost createModuleSpaceHost(ModuleHandle module, String moduleName) throws UnableToCompleteException {

        TreeLogger logger = module.getLogger();
        try {
            ModuleDef moduleDef = loadModule(logger, moduleName, true);
            CompilationState compilationState = moduleDef.getCompilationState(logger);
            ArtifactAcceptor artifactAcceptor = createArtifactAcceptor(logger, moduleDef);
            // TODO: do we want to handle rebind cache ?
            return new ShellModuleSpaceHost(logger, compilationState, moduleDef, genDir, artifactAcceptor, null);

        } catch (RuntimeException e) {
            logger.log(TreeLogger.ERROR, "Exception initializing module", e);
            module.unload();
            throw e;
        }
    }

    private ModuleHandle getModuleHandle() {
        return new ModuleHandle() {

            @Override
            public void unload() {

            }

            @Override
            public TreeLogger getLogger() {
                return new ConsoleTreeLogger();
            }
        };
    }

    /**
     * Load a module.
     * 
     * @param moduleName name of the module to load
     * @param logger TreeLogger to use
     * @param refresh if <code>true</code>, refresh the module from disk
     * @return the loaded module
     * @throws UnableToCompleteException
     */
    private ModuleDef loadModule(TreeLogger logger, String moduleName, boolean refresh) throws UnableToCompleteException {
        ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger, moduleName, refresh);
        Validate.stateNotNull(moduleDef, "Required module state is absent");
        return moduleDef;
    }

    private ArtifactAcceptor createArtifactAcceptor(TreeLogger logger, final ModuleDef module) throws UnableToCompleteException {
        final StandardLinkerContext linkerContext = link(logger, module);
        return new ArtifactAcceptor() {
            public void accept(TreeLogger relinkLogger, ArtifactSet newArtifacts) throws UnableToCompleteException {
                relink(relinkLogger, linkerContext, module, newArtifacts);
            }
        };
    }

    /**
     * Perform an initial hosted mode link, without overwriting newer or unmodified files in the output folder.
     * 
     * @param logger the logger to use
     * @param module the module to link
     * @throws UnableToCompleteException
     */
    private StandardLinkerContext link(TreeLogger logger, ModuleDef module) throws UnableToCompleteException {
        TreeLogger linkLogger = logger.branch(TreeLogger.DEBUG, "Linking module '" + module.getName() + "'");

        // Create a new active linker stack for the fresh link.
        StandardLinkerContext linkerStack = new StandardLinkerContext(linkLogger, module, jjsoptions);
        ArtifactSet artifacts = linkerStack.getArtifactsForPublicResources(logger, module);
        artifacts = linkerStack.invokeLegacyLinkers(linkLogger, artifacts);
        artifacts = linkerStack.invokeFinalLink(linkLogger, artifacts);
        produceOutput(linkLogger, linkerStack, artifacts, module, false);
        return linkerStack;
    }

    /**
     * Perform hosted mode relink when new artifacts are generated, without overwriting newer or unmodified files in the output
     * folder.
     * 
     * @param logger the logger to use
     * @param module the module to link
     * @param newlyGeneratedArtifacts the set of new artifacts
     * @throws UnableToCompleteException
     */
    private void relink(TreeLogger logger, StandardLinkerContext linkerContext, ModuleDef module,
            ArtifactSet newlyGeneratedArtifacts) throws UnableToCompleteException {
        TreeLogger linkLogger = logger.branch(TreeLogger.DEBUG, "Relinking module '" + module.getName() + "'");

        ArtifactSet artifacts = linkerContext.invokeRelink(linkLogger, newlyGeneratedArtifacts);
        produceOutput(linkLogger, linkerContext, artifacts, module, true);
    }

    protected void produceOutput(TreeLogger logger, StandardLinkerContext linkerStack, ArtifactSet artifacts, ModuleDef module,
            boolean isRelink) throws UnableToCompleteException {

    }

    /**
     * Produce a random string that has low probability of collisions.
     * 
     * <p>
     * In this case, we use 16 characters, each drawn from a pool of 94, so the number of possible values is 94^16, leading to
     * an expected number of values used before a collision occurs as sqrt(pi/2) * 94^8 (treated the same as a birthday attack),
     * or a little under 10^16.
     * 
     * <p>
     * This algorithm is also implemented in hosted.html, though it is not technically important that they match.
     * 
     * @return a random string
     */
    private static String randomString() {
        StringBuilder buf = new StringBuilder(16);
        for (int i = 0; i < 16; ++i) {
            buf.append((char) RNG.nextInt('~' - '!' + 1) + '!');
        }
        return buf.toString();
    }
}

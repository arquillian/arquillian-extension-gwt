package org.jboss.arquillian.gwt;

import java.io.File;

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
import com.google.gwt.dev.shell.ShellModuleSpaceHost;

public class ShellModuleSpaceHostUtil {

    // FIXME GENDIR MUST BE SET
    private File genDir = new File(".");

    // FIXME THIS SHOULD GO TO ARQUILLIAN
    private JJSOptions jjsoptions = new JJSOptionsImpl();

    public ShellModuleSpaceHost createModuleSpaceHost(String moduleName) throws UnableToCompleteException {
        return createModuleSpaceHost(new ModuleHandle() {

            @Override
            public void unload() {

            }

            @Override
            public TreeLogger getLogger() {
                return new ConsoleTreeLogger();
            }
        }, moduleName);
    }

    public ShellModuleSpaceHost createModuleSpaceHost(ModuleHandle handle, String moduleName) throws UnableToCompleteException {

        TreeLogger logger = handle.getLogger();
        ModuleDef module = loadModule(logger, moduleName, true);
        CompilationState compilationState = module.getCompilationState(logger);
        ArtifactAcceptor artifactAcceptor = createArtifactAcceptor(logger, module);
        // TODO: do we want to handle rebind cache ?
        return new ShellModuleSpaceHost(logger, compilationState, module, genDir, artifactAcceptor, null);
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

}

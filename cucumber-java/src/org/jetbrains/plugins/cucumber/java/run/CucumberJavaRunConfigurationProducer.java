package org.jetbrains.plugins.cucumber.java.run;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.execution.junit2.info.LocationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Andrey.Vokin
 * @since 8/6/12
 */
public abstract class CucumberJavaRunConfigurationProducer extends JavaRunConfigurationProducerBase<CucumberJavaRunConfiguration> implements Cloneable {
  public static final String FORMATTER_OPTIONS = " --format org.jetbrains.plugins.cucumber.java.run.CucumberJvmSMFormatter --monochrome";
  public static final String CUCUMBER_1_0_MAIN_CLASS = "cucumber.cli.Main";
  public static final String CUCUMBER_1_1_MAIN_CLASS = "cucumber.api.cli.Main";

  protected CucumberJavaRunConfigurationProducer() {
    super(CucumberJavaRunConfigurationType.getInstance());
  }

  @Nullable
  protected abstract NullableComputable<String> getGlue(@NotNull final PsiElement element);

  protected abstract String getConfigurationName(@NotNull ConfigurationContext context);

  protected String getNameFilter(@NotNull ConfigurationContext context) {
    return "";
  }

  @Nullable
  protected abstract VirtualFile getFileToRun(ConfigurationContext context);

  @Override
  protected boolean setupConfigurationFromContext(CucumberJavaRunConfiguration configuration,
                                                  ConfigurationContext context,
                                                  Ref<PsiElement> sourceElement) {
    final VirtualFile virtualFile = getFileToRun(context);
    if (virtualFile == null) {
      return false;
    }

    final Project project = configuration.getProject();
    final PsiElement element = context.getPsiLocation();

    if (element == null) {
      return false;
    }

    final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
    if (module == null) return false;

    String mainClassName = null;
    final Location location = context.getLocation();
    if (location != null) {
      if (LocationUtil.isJarAttached(location, CUCUMBER_1_0_MAIN_CLASS, new PsiDirectory[0])) {
        mainClassName = CUCUMBER_1_0_MAIN_CLASS;
      } else if (LocationUtil.isJarAttached(location, CUCUMBER_1_1_MAIN_CLASS, new PsiDirectory[0])) {
        mainClassName = CUCUMBER_1_1_MAIN_CLASS;
      }
    }
    if (mainClassName == null) {
      return false;
    }

    final VirtualFile file = getFileToRun(context);
    if (file == null) {
      return false;
    }
    if (StringUtil.isEmpty(configuration.getGlue())) {
      final NullableComputable<String> glue = getGlue(element);
      configuration.setGlue(glue);
    }
    configuration.setNameFilter(getNameFilter(context));
    configuration.setFilePath(file.getPath());
    configuration.setProgramParameters(FORMATTER_OPTIONS);
    if (StringUtil.isEmpty(configuration.MAIN_CLASS_NAME)) {
      configuration.MAIN_CLASS_NAME = mainClassName;
    }

    if (configuration.getNameFilter() != null && configuration.getNameFilter().length() > 0) {
      final String newProgramParameters = configuration.getProgramParameters() + " --name \"" + configuration.getNameFilter() + "\"";
      configuration.setProgramParameters(newProgramParameters);
    }

    configuration.myGeneratedName = getConfigurationName(context);
    configuration.setGeneratedName();

    setupConfigurationModule(context, configuration);
    JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(CucumberJavaRunConfiguration runConfiguration, ConfigurationContext context) {
    final Location location = JavaExecutionUtil.stepIntoSingleClass(context.getLocation());

    final VirtualFile fileToRun = getFileToRun(context);
    if (fileToRun == null) {
      return false;
    }

    if (!fileToRun.getPath().equals(runConfiguration.getFilePath())) {
      return false;
    }

    if (!Comparing.strEqual(getNameFilter(context), runConfiguration.getNameFilter())) {
      return false;
    }

    final Module configurationModule = runConfiguration.getConfigurationModule().getModule();
    if (!Comparing.equal(location.getModule(), configurationModule)) {
      return false;
    }

    return true;
  }
}

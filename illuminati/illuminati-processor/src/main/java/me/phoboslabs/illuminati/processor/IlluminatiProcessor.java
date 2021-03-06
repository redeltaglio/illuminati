package me.phoboslabs.illuminati.processor;

import com.google.auto.service.AutoService;
import me.phoboslabs.illuminati.annotation.Illuminati;
import me.phoboslabs.illuminati.common.properties.IlluminatiPropertiesHelper;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static me.phoboslabs.illuminati.common.constant.IlluminatiConstant.*;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 10/07/2017.
 */
@AutoService(Processor.class)
public class IlluminatiProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    private String generatedIlluminatiTemplate;

    @Override public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<>();
        annotataions.add(Illuminati.class.getCanonicalName());
        return annotataions;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;// SourceVersion.latestSupported();
    }

    private static final List<ElementKind> ANNOTATION_ELEMENT_KIND = Collections.unmodifiableList(Arrays.asList(ElementKind.CLASS, ElementKind.METHOD));

    @Override public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env)  {
        this.messager.printMessage(Kind.WARNING, "start illuminati compile");
        outerloop:
        for (TypeElement typeElement : typeElements) {
            for (Element element : env.getElementsAnnotatedWith(typeElement)) {
                Illuminati illuminati = element.getAnnotation(Illuminati.class);

                if (illuminati == null) {
                    continue;
                }

                if (ANNOTATION_ELEMENT_KIND.contains(element.getKind()) == false) {
                    this.messager.printMessage(Kind.ERROR, "The class %s is not class or method."+ element.getSimpleName());
                    break outerloop;
                }

                final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);

                if (pkg == null) {
                    this.messager.printMessage(Kind.ERROR, "Sorry, basePackage is wrong in properties read process.");
                    break outerloop;
                }

                if (this.setGeneratedIlluminatiTemplate(pkg.toString()) == false) {
                    continue;
                }

                try {
                    final JavaFileObject javaFile = this.filer.createSourceFile("IlluminatiPointcutGenerated");
                    final Writer writer = javaFile.openWriter();

                    if (writer != null) {
                        writer.write(this.generatedIlluminatiTemplate);
                        writer.close();
                        this.messager.printMessage(Kind.NOTE, "generate source code!!");
                    } else {
                        this.messager.printMessage(Kind.ERROR, "Sorry, something is wrong in writer 'IlluminatiPointcutGenerated.java' process.");
                    }

                    // IlluminatiPointcutGenerated must exists only one on classloader.
                    break outerloop;
                } catch (IOException e) {
                    this.messager.printMessage(Kind.ERROR, "Sorry, something is wrong in generated 'IlluminatiPointcutGenerated.java' process.");
                    break outerloop;
                }
            }
        }

        return true;
    }

    /**
     * Prints an error message
     *
     * @param e The element which has caused the error. Can be null
     * @param msg The error message
     */
    public void error(Element e, String msg) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    /**
     * Generated the Illuminati Client class body.
     *
     * @param basePackageName assign a properties file setting dto. Can not be null
     * @return boolean if failed is false and another is true.
     */
    private boolean setGeneratedIlluminatiTemplate (final String basePackageName) {
        // step 1.  set basicImport
        this.generatedIlluminatiTemplate = "package {basePackageName};\r\n".concat(this.getImport());
        // step 2.  base package name
        this.generatedIlluminatiTemplate = this.generatedIlluminatiTemplate.replace("{basePackageName}", basePackageName);

        final String staticConfigurationTemplate = "     private final IlluminatiClientInit illuminatiClientInit;\r\n \r\n";
        final String illuminatiAnnotationName = "me.phoboslabs.illuminati.annotation.Illuminati";
        // step 3.  check chaosBomber is activated.
        PropertiesHelper propertiesHelper = new PropertiesHelper(this.messager);
        final String checkChaosBomber = propertiesHelper.getPropertiesValueByKey("chaosBomber", "false");

        String illuminatiExecuteMethod = "";

        if (StringObjectUtils.isValid(checkChaosBomber) && "true".equalsIgnoreCase(checkChaosBomber)) {
            illuminatiExecuteMethod = "ByChaosBomber";
        }

        // step 4. set the method body
        this.generatedIlluminatiTemplate += ""
                + "@Component\r\n"
                + "@Aspect\r\n"
                + "public class IlluminatiPointcutGenerated {\r\n\r\n"
                + staticConfigurationTemplate
                + "     public IlluminatiPointcutGenerated() {\r\n"
                + "         this.illuminatiClientInit = IlluminatiClientInit.getInstance();\r\n"
                + "     }\r\n\r\n"
                + "     @Pointcut(\"@within("+illuminatiAnnotationName+") || @annotation("+illuminatiAnnotationName+")\")\r\n"
                + "     public void illuminatiPointcutMethod () { }\r\n\r\n"

                + "     @Around(\"illuminatiPointcutMethod()\")\r\n"
                + "     public Object profile (ProceedingJoinPoint pjp) throws Throwable {\r\n"
                + "         if (this.illuminatiClientInit.illuminatiIsInitialized() == false) {\n"
                + "           return pjp.proceed();\n"
                + "         }\n"
                + "         if (illuminatiClientInit.checkIlluminatiIsIgnore(pjp)) {\r\n"
                + "             return pjp.proceed();\r\n"
                + "         }\r\n"
                + "         HttpServletRequest request = null;\r\n"
                + "         try {\r\n"
                + "             request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();\r\n"
                + "         } catch (Exception ignore) {}\r\n"
                + "         return illuminatiClientInit.executeIlluminati"+illuminatiExecuteMethod+"(pjp, request);\r\n"
                + "     }\r\n"
                + "}\r\n"
                ;

        return true;
    }

    private String getImport () {
        final String[] illuminatis = {
                "init.IlluminatiClientInit"
        };

        final String[] aspectjs = {
                "annotation.Aspect",
                "ProceedingJoinPoint",
                "annotation.Around",
                "annotation.Pointcut"
        };

        final String[] springs = {
                "stereotype.Component",
                "web.context.request.RequestContextHolder",
                "web.context.request.ServletRequestAttributes"
        };

        final String[] blanks = {
                "javax.servlet.http.HttpServletRequest"
        };

        final Map<String, String[]> imports = new HashMap<>();
        imports.put("me.phoboslabs.illuminati.processor", illuminatis);
        imports.put("org.aspectj.lang", aspectjs);
        imports.put("org.springframework", springs);
        imports.put("", blanks);

        final StringBuilder importString = new StringBuilder();

        for (Map.Entry<String, String[]> entry : imports.entrySet() ) {
            for (String importLib : entry.getValue()) {
                importString.append("import ");
                importString.append(entry.getKey());

                if ("".equals(entry.getKey()) == false) {
                    importString.append(".");
                }

                importString.append(importLib);
                importString.append(";\r\n");
            }
        }

        return importString.toString();
    }

    /**
     *
     */
    private class PropertiesHelper {

        private final static String DEFAULT_CONFIG_PROPERTIES_FILE_NAME = "illuminati";

        private final Messager messager;

        PropertiesHelper (Messager messager) {
            this.messager = messager;
        }

        public String getPropertiesValueByKey (final String key, final String defaultValue) {
            final IlluminatiProcessorPropertiesImpl illuminatiProperties = this.getIlluminatiProperties();
            if (illuminatiProperties == null) {
                return defaultValue;
            }
            String propertiesValue = null;

            if (StringObjectUtils.isValid(key)) {
                try {
                    final String methodName = "get".concat(key.substring(0, 1).toUpperCase()).concat(key.substring(1));
                    final Method getNameMethod = IlluminatiProcessorPropertiesImpl.class.getMethod(methodName);
                    propertiesValue = (String) getNameMethod.invoke(illuminatiProperties);
                }
                catch (Exception ex) {
                    this.messager.printMessage(Diagnostic.Kind.WARNING, "Sorry, unable to find method. (" + ex.toString() + ")");
                }
            }

            return (StringObjectUtils.isValid(propertiesValue)) ? propertiesValue : defaultValue;
        }

        private IlluminatiProcessorPropertiesImpl getIlluminatiProperties () {
            IlluminatiProcessorPropertiesImpl illuminatiProperties = null;

            for (String extension : CONFIG_FILE_EXTENSTIONS) {
                StringBuilder dotBeforeExtension = new StringBuilder(".");

                if (StringObjectUtils.isValid(PROFILES_PHASE)) {
                    dotBeforeExtension.append("-");
                    dotBeforeExtension.append(PROFILES_PHASE);
                    dotBeforeExtension.append(".");
                }

                StringBuilder fullFileName = new StringBuilder(DEFAULT_CONFIG_PROPERTIES_FILE_NAME);
                fullFileName.append(dotBeforeExtension.toString());
                fullFileName.append(extension);

                illuminatiProperties = getIlluminatiPropertiesByFile(fullFileName.toString());

                if (illuminatiProperties != null) {
                    break;
                }
            }

            if (illuminatiProperties == null) {
                illuminatiProperties = getIlluminatiPropertiesFromBasicFiles();
            }

            if (illuminatiProperties == null) {
                this.messager.printMessage(Diagnostic.Kind.WARNING, "Sorry, unable to find config file");
            }

            return illuminatiProperties;
        }
    }

    private IlluminatiProcessorPropertiesImpl getIlluminatiPropertiesByFile(final String configPropertiesFileName) {
        final InputStream input = IlluminatiPropertiesHelper.class.getClassLoader().getResourceAsStream(configPropertiesFileName);

        if (input == null) {
            return null;
        }

        IlluminatiProcessorPropertiesImpl illuminatiProperties = null;
        try {
            if (configPropertiesFileName.contains(".yml") || configPropertiesFileName.contains(".yaml")) {
                illuminatiProperties = YAML_MAPPER.readValue(input, IlluminatiProcessorPropertiesImpl.class);
            } else {
                final Properties prop = new Properties();
                prop.load(input);
                illuminatiProperties = new IlluminatiProcessorPropertiesImpl(prop);
            }
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING, "Sorry, something is wrong in read process. (" + ex.toString() + ")");
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                this.messager.printMessage(Diagnostic.Kind.WARNING, "Sorry, something is wrong in close InputStream process. (" + ex.toString() + ")");
            }
        }

        return illuminatiProperties;
    }

    private IlluminatiProcessorPropertiesImpl getIlluminatiPropertiesFromBasicFiles() {
        IlluminatiProcessorPropertiesImpl illuminatiProperties;

        for (String fileName : BASIC_CONFIG_FILES) {
            illuminatiProperties = getIlluminatiPropertiesByFile(fileName);

            if (illuminatiProperties != null) {
                return illuminatiProperties;
            }
        }

        return null;
    }

}

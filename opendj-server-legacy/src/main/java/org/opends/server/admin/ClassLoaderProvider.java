/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2008-2009 Sun Microsystems, Inc.
 * Portions Copyright 2012-2015 ForgeRock AS.
 */
package org.opends.server.admin;



import static org.opends.messages.AdminMessages.*;
import static org.opends.messages.ExtensionMessages.*;
import static org.opends.server.util.StaticUtils.*;
import static org.opends.server.util.ServerConstants.EOL;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.server.admin.std.meta.RootCfgDefn;
import org.opends.server.core.DirectoryServer;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.opends.server.types.InitializationException;


/**
 * Manages the class loader which should be used for loading configuration definition classes and associated extensions.
 * <p>
 * For extensions which define their own extended configuration definitions, the class loader will make sure
 * that the configuration definition classes are loaded and initialized.
 * <p>
 * Initially the class loader provider is disabled, and calls to the {@link #getClassLoader()} will return
 * the system default class loader.
 * <p>
 * Applications <b>MUST NOT</b> maintain persistent references to the class loader as it can change at run-time.
 */
public final class ClassLoaderProvider {
  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /**
   * Private URLClassLoader implementation.
   * This is only required so that we can provide access to the addURL method.
   */
  private static final class MyURLClassLoader extends URLClassLoader {

    /** Create a class loader with the default parent class loader. */
    public MyURLClassLoader() {
      super(new URL[0]);
    }



    /**
     * Create a class loader with the provided parent class loader.
     *
     * @param parent
     *          The parent class loader.
     */
    public MyURLClassLoader(ClassLoader parent) {
      super(new URL[0], parent);
    }



    /**
     * Add a Jar file to this class loader.
     *
     * @param jarFile
     *          The name of the Jar file.
     * @throws MalformedURLException
     *           If a protocol handler for the URL could not be found, or if some other error occurred
     *           while constructing the URL.
     * @throws SecurityException
     *           If a required system property value cannot be accessed.
     */
    public void addJarFile(File jarFile) throws SecurityException, MalformedURLException {
      addURL(jarFile.toURI().toURL());
    }

  }

  /** The name of the manifest file listing the core configuration definition classes. */
  private static final String CORE_MANIFEST = "core.manifest";

  /** The name of the manifest file listing a extension's configuration definition classes. */
  private static final String EXTENSION_MANIFEST = "extension.manifest";

  /** The name of the lib directory. */
  private static final String LIB_DIR = "lib";

  /** The name of the extensions directory. */
  private static final String EXTENSIONS_DIR = "extensions";

  /** The singleton instance. */
  private static final ClassLoaderProvider INSTANCE = new ClassLoaderProvider();

  /** Attribute name in jar's MANIFEST corresponding to the revision number. */
  private static final String REVISION_NUMBER = "Revision-Number";

  /** The attribute names for build information is name, version and revision number. */
  private static final String[] BUILD_INFORMATION_ATTRIBUTE_NAMES =
                 new String[]{Attributes.Name.EXTENSION_NAME.toString(),
                              Attributes.Name.IMPLEMENTATION_VERSION.toString(),
                              REVISION_NUMBER};


  /**
   * Get the single application wide class loader provider instance.
   *
   * @return Returns the single application wide class loader provider instance.
   */
  public static ClassLoaderProvider getInstance() {
    return INSTANCE;
  }

  /** Set of registered Jar files. */
  private Set<File> jarFiles = new HashSet<>();

  /**
   * Underlying class loader used to load classes and resources (null if disabled).<br>
   * We contain a reference to the URLClassLoader rather than sub-class it so that it is possible to replace the
   * loader at run-time. For example, when removing or replacing extension Jar files (the URLClassLoader
   * only supports adding new URLs, not removal).
   */
  private MyURLClassLoader loader;



  /** Private constructor. */
  private ClassLoaderProvider() {
    // No implementation required.
  }



  /**
   * Disable this class loader provider and removed any registered extensions.
   *
   * @throws IllegalStateException
   *           If this class loader provider is already disabled.
   */
  public synchronized void disable()
      throws IllegalStateException {
    if (loader == null) {
      throw new IllegalStateException(
          "Class loader provider already disabled.");
    }
    loader = null;
    jarFiles = new HashSet<>();
  }



  /**
   * Enable this class loader provider using the application's class loader as the parent class loader.
   *
   * @throws InitializationException
   *           If the class loader provider could not initialize successfully.
   * @throws IllegalStateException
   *           If this class loader provider is already enabled.
   */
  public synchronized void enable()
      throws InitializationException, IllegalStateException {
    enable(RootCfgDefn.class.getClassLoader());
  }



  /**
   * Enable this class loader provider using the provided parent class loader.
   *
   * @param parent
   *          The parent class loader.
   * @throws InitializationException
   *           If the class loader provider could not initialize successfully.
   * @throws IllegalStateException
   *           If this class loader provider is already enabled.
   */
  public synchronized void enable(ClassLoader parent)
      throws InitializationException, IllegalStateException {
    if (loader != null) {
      throw new IllegalStateException("Class loader provider already enabled.");
    }

    if (parent != null) {
      loader = new MyURLClassLoader(parent);
    } else {
      loader = new MyURLClassLoader();
    }

    // Forcefully load all configuration definition classes in OpenDJ.jar.
    initializeCoreComponents();

    // Put extensions jars into the class loader and load all configuration definition classes in that they contain.
    // First load the extension from the install directory, then from the instance directory.
    File installExtensionsPath  = buildExtensionPath(DirectoryServer.getServerRoot());
    File instanceExtensionsPath = buildExtensionPath(DirectoryServer.getInstanceRoot());

    initializeAllExtensions(installExtensionsPath);

    if (! installExtensionsPath.getAbsolutePath().equals(instanceExtensionsPath.getAbsolutePath())) {
      initializeAllExtensions(instanceExtensionsPath);
    }
  }

  private File buildExtensionPath(String directory)  {
    File libDir = new File(directory, LIB_DIR);
    try {
      return new File(libDir, EXTENSIONS_DIR).getCanonicalFile();
    } catch (Exception e) {
      return new File(libDir, EXTENSIONS_DIR);
    }
  }


  /**
   * Gets the class loader which should be used for loading classes and resources. When this class loader provider
   * is disabled, the system default class loader will be returned by default.
   * <p>
   * Applications <b>MUST NOT</b> maintain persistent references to the class loader as it can change at run-time.
   *
   * @return Returns the class loader which should be used for loading classes and resources.
   */
  public synchronized ClassLoader getClassLoader() {
    if (loader != null) {
      return loader;
    } else {
      return ClassLoader.getSystemClassLoader();
    }
  }



  /**
   * Indicates whether this class loader provider is enabled.
   *
   * @return Returns <code>true</code> if this class loader provider is enabled.
   */
  public synchronized boolean isEnabled() {
    return loader != null;
  }



  /**
   * Add the named extensions to this class loader.
   *
   * @param extensions
   *          A List of the names of the extensions to be loaded.
   * @throws InitializationException
   *           If one of the extensions could not be loaded and initialized.
   */
  private synchronized void addExtension(List<File> extensions)
      throws InitializationException {
    // First add the Jar files to the class loader.
    List<JarFile> jars = new LinkedList<>();
    for (File extension : extensions) {
      if (jarFiles.contains(extension)) {
        // Skip this file as it is already loaded.
        continue;
      }

      // Attempt to load it.
      jars.add(loadJarFile(extension));

      // Register the Jar file with the class loader.
      try {
        loader.addJarFile(extension);
      } catch (Exception e) {
        logger.traceException(e);

        LocalizableMessage message = ERR_ADMIN_CANNOT_OPEN_JAR_FILE
            .get(extension.getName(), extension.getParent(), stackTraceToSingleLineString(e));
        throw new InitializationException(message);
      }
      jarFiles.add(extension);
    }

    // Now forcefully load the configuration definition classes.
    for (JarFile jar : jars) {
      initializeExtension(jar);
    }
  }



  /**
   * Prints out all information about extensions.
   *
   * @return a String instance representing all information about extensions;
   *         <code>null</code> if there is no information available.
   */
  public String printExtensionInformation() {
    File extensionsPath = buildExtensionPath(DirectoryServer.getServerRoot());

    List<File> extensions = new ArrayList<>();
    if (extensionsPath.exists() && extensionsPath.isDirectory()) {
      extensions.addAll(listFiles(extensionsPath));
    }

    File instanceExtensionsPath = buildExtensionPath(DirectoryServer.getInstanceRoot());
    if (!extensionsPath.getAbsolutePath().equals(instanceExtensionsPath.getAbsolutePath())) {
      extensions.addAll(listFiles(instanceExtensionsPath));
    }

    if ( extensions.isEmpty() ) {
      return null;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    // prints:
    // --
    //            Name                 Build number         Revision number
    ps.printf("--%s           %-20s %-20s %-20s%s",
              EOL,
              "Name",
              "Build number",
              "Revision number",
              EOL);

    for(File extension : extensions) {
      printExtensionDetails(ps, extension);
    }

    return baos.toString();
  }

  private List<File> listFiles(File path){
    if (path.exists() && path.isDirectory()) {
      return Arrays.asList(path.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          // only files with names ending with ".jar"
          return pathname.isFile() && pathname.getName().endsWith(".jar");
        }
      }));
    }
    return Collections.emptyList();
  }

  private void printExtensionDetails(PrintStream ps, File extension) {
    // retrieve MANIFEST entry and display name, build number and revision number
    try {
      JarFile jarFile = new JarFile(extension);
      JarEntry entry = jarFile.getJarEntry("admin/" + EXTENSION_MANIFEST);
      if (entry == null) {
        return;
      }

      String[] information = getBuildInformation(jarFile);

      ps.append("Extension: ");
      boolean addBlank = false;
      for(String name : information) {
        if ( addBlank ) {
          ps.append(" ");
        } else {
          addBlank = true;
        }
        ps.printf("%-20s", name);
      }
      ps.append(EOL);
    } catch(Exception e) {
      // ignore extra information for this extension
    }
  }


  /**
   * Returns a String array with the following information :
   * <br>index 0: the name of the extension.
   * <br>index 1: the build number of the extension.
   * <br>index 2: the revision number of the extension.
   *
   * @param extension the jar file of the extension
   * @return a String array containing the name, the build number and the revision number
   *         of the extension given in argument
   * @throws java.io.IOException thrown if the jar file has been closed.
   */
  private String[] getBuildInformation(JarFile extension)
      throws IOException {
    String[] result = new String[3];

    // retrieve MANIFEST entry and display name, version and revision
    Manifest manifest = extension.getManifest();

    if ( manifest != null ) {
      Attributes attributes = manifest.getMainAttributes();

      int index = 0;
      for(String name : BUILD_INFORMATION_ATTRIBUTE_NAMES) {
        String value = attributes.getValue(name);
        if ( value == null ) {
          value = "<unknown>";
        }
        result[index++] = value;
      }
    }

    return result;
  }



  /**
   * Put extensions jars into the class loader and load all configuration definition classes in that they contain.
   * @param extensionsPath Indicates where extensions are located.
   *
   * @throws InitializationException
   *           If the extensions folder could not be accessed or if a extension jar file could not be accessed or
   *           if one of the configuration definition classes could not be initialized.
   */
  private void initializeAllExtensions(File extensionsPath)
      throws InitializationException {

    try {
      if (!extensionsPath.exists()) {
        // The extensions directory does not exist. This is not a critical problem.
        logger.warn(WARN_ADMIN_NO_EXTENSIONS_DIR, extensionsPath);
        return;
      }

      if (!extensionsPath.isDirectory()) {
        // The extensions directory is not a directory. This is more critical.
        throw new InitializationException(ERR_ADMIN_EXTENSIONS_DIR_NOT_DIRECTORY.get(extensionsPath));
      }

      // Add and initialize the extensions.
      addExtension(listFiles(extensionsPath));
    } catch (InitializationException e) {
      logger.traceException(e);
      throw e;
    } catch (Exception e) {
      logger.traceException(e);

      LocalizableMessage message = ERR_ADMIN_EXTENSIONS_CANNOT_LIST_FILES.get(
          extensionsPath, stackTraceToSingleLineString(e));
      throw new InitializationException(message, e);
    }
  }



  /**
   * Make sure all core configuration definitions are loaded.
   *
   * @throws InitializationException
   *           If the core manifest file could not be read or if one of the configuration definition
   *           classes could not be initialized.
   */
  private void initializeCoreComponents()
      throws InitializationException {
    InputStream is = RootCfgDefn.class.getResourceAsStream("/admin/" + CORE_MANIFEST);

    if (is == null) {
      LocalizableMessage message = ERR_ADMIN_CANNOT_FIND_CORE_MANIFEST.get(CORE_MANIFEST);
      throw new InitializationException(message);
    }

    try {
      loadDefinitionClasses(is);
    } catch (InitializationException e) {
      logger.traceException(e);

      LocalizableMessage message = ERR_CLASS_LOADER_CANNOT_LOAD_CORE.get(CORE_MANIFEST,
          stackTraceToSingleLineString(e));
      throw new InitializationException(message);
    }
  }



  /**
   * Make sure all the configuration definition classes in a extension are loaded.
   *
   * @param jarFile
   *          The extension's Jar file.
   * @throws InitializationException
   *           If the extension jar file could not be accessed or if one of the configuration definition classes
   *           could not be initialized.
   */
  private void initializeExtension(JarFile jarFile)
      throws InitializationException {
    JarEntry entry = jarFile.getJarEntry("admin/" + EXTENSION_MANIFEST);
    if (entry != null) {
      InputStream is;
      try {
        is = jarFile.getInputStream(entry);
      } catch (Exception e) {
        logger.traceException(e);

        LocalizableMessage message = ERR_ADMIN_CANNOT_READ_EXTENSION_MANIFEST.get(EXTENSION_MANIFEST, jarFile.getName(),
            stackTraceToSingleLineString(e));
        throw new InitializationException(message);
      }

      try {
        loadDefinitionClasses(is);
      } catch (InitializationException e) {
        logger.traceException(e);

        LocalizableMessage message = ERR_CLASS_LOADER_CANNOT_LOAD_EXTENSION.get(jarFile.getName(), EXTENSION_MANIFEST,
            stackTraceToSingleLineString(e));
        throw new InitializationException(message);
      }
      logExtensionsBuildInformation(jarFile);
    }
  }



  private void logExtensionsBuildInformation(JarFile jarFile)
  {
    try {
      String[] information = getBuildInformation(jarFile);
      LocalizedLogger extensionsLogger = LocalizedLogger.getLocalizedLogger("org.opends.server.extensions");
      extensionsLogger.info(NOTE_LOG_EXTENSION_INFORMATION, jarFile.getName(), information[1], information[2]);
    } catch(Exception e) {
      // Do not log information for that extension
    }
  }



  /**
   * Forcefully load configuration definition classes named in a manifest file.
   *
   * @param is
   *          The manifest file input stream.
   * @throws InitializationException
   *           If the definition classes could not be loaded and initialized.
   */
  private void loadDefinitionClasses(InputStream is)
      throws InitializationException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    List<AbstractManagedObjectDefinition<?, ?>> definitions = new LinkedList<>();
    while (true) {
      String className;
      try {
        className = reader.readLine();
      } catch (IOException e) {
        throw new InitializationException(
            ERR_CLASS_LOADER_CANNOT_READ_MANIFEST_FILE.get(e.getMessage()), e);
      }

      // Break out when the end of the manifest is reached.
      if (className == null) {
        break;
      }

      // Skip blank lines.
      className = className.trim();
      if (className.length() == 0) {
        continue;
      }

      // Skip lines beginning with #.
      if (className.startsWith("#")) {
        continue;
      }

      logger.trace("Loading class " + className);

      // Load the class and get an instance of it if it is a definition.
      Class<?> theClass;
      try {
        theClass = Class.forName(className, true, loader);
      } catch (Exception e) {
        throw new InitializationException(ERR_CLASS_LOADER_CANNOT_LOAD_CLASS.get(className, e.getMessage()), e);
      }
      if (AbstractManagedObjectDefinition.class.isAssignableFrom(theClass)) {
        // We need to instantiate it using its getInstance() static method.
        Method method;
        try {
          method = theClass.getMethod("getInstance");
        } catch (Exception e) {
          throw new InitializationException(
              ERR_CLASS_LOADER_CANNOT_FIND_GET_INSTANCE_METHOD.get(className, e.getMessage()), e);
        }

        // Get the definition instance.
        AbstractManagedObjectDefinition<?, ?> d;
        try {
          d = (AbstractManagedObjectDefinition<?, ?>) method.invoke(null);
        } catch (Exception e) {
          throw new InitializationException(
              ERR_CLASS_LOADER_CANNOT_INVOKE_GET_INSTANCE_METHOD.get(className, e.getMessage()), e);
        }
        definitions.add(d);
      }
    }

    // Initialize any definitions that were loaded.
    for (AbstractManagedObjectDefinition<?, ?> d : definitions) {
      try {
        d.initialize();
      } catch (Exception e) {
        throw new InitializationException(
            ERR_CLASS_LOADER_CANNOT_INITIALIZE_DEFN.get(d.getName(), d.getClass().getName(), e.getMessage()), e);
      }
    }
  }



  /**
   * Load the named Jar file.
   *
   * @param jar
   *          The name of the Jar file to load.
   * @return Returns the loaded Jar file.
   * @throws InitializationException
   *           If the Jar file could not be loaded.
   */
  private JarFile loadJarFile(File jar)
      throws InitializationException {
    JarFile jarFile;

    try {
      // Load the extension jar file.
      jarFile = new JarFile(jar);
    } catch (Exception e) {
      logger.traceException(e);

      LocalizableMessage message = ERR_ADMIN_CANNOT_OPEN_JAR_FILE.get(
          jar.getName(), jar.getParent(), stackTraceToSingleLineString(e));
      throw new InitializationException(message);
    }
    return jarFile;
  }

}

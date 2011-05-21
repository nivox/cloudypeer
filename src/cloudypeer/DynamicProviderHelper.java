/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package cloudypeer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Helper class that simplify the task of locating and instantiating providers of an interface (or
 * any base class) in a dynamic fashion.
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 * @version 1.0
 */
public final class DynamicProviderHelper {

  private DynamicProviderHelper() {}

  /**
   * Read providers configuration from input stream
   *
   * @param base Name of the base class
   * @param in InputStream pointing to a providers configuration properties file
   * @param resource Name on the resource to read
   * @return Providers configuration
   */
  private static <E> HashMap<String, Class<? extends E>>
   readProvidersConfiguration(Class<E> base, InputStream in, String resource)
  {
    HashMap<String, Class<? extends E>> providers = new HashMap<String, Class<? extends E>>();
    Properties conf = new Properties();
    try{
      conf.load(in);
    } catch (Exception e) {
      System.err.format("Error while reading configuration for %s (%s)\n",
                        base.getName(), resource);
      e.printStackTrace();
    } finally {
      try {
        in.close();
      } catch (IOException e) {}
    }

    for (Map.Entry entry: conf.entrySet()) {
      String key = (String) entry.getKey();
      String className = (String) entry.getValue();

      try {
        Class<? extends E> clazz = (Class<? extends E>) Class.forName(className);
        providers.put(key, clazz);
      } catch (ClassNotFoundException e) {
        System.err.format("Provider implementation not found for %s (%s): %s\n",
                          base.getName(), resource, className);
      } catch (ClassCastException e) {
        System.err.format("Invalid provider implementation for %s (%s): %s\n",
                          base.getName(), resource, className);
      }
    }

    return providers;
  }

  /**
   * Build the provider map for the specified base class. <br>
   * First tries to read the built-in configuration located in the same package of the base
   * class. Then tries to load external configuration located in the root of the classpath. <br>
   * <br>
   * The properties file must respect the following format: <br>
   * <code>provider-name=fully.qualified.provider.class</code>
   *
   * @param base Base class type of the provider
   * @param confPropertiesName Name of the properties file holding the providers configuration
   * @return Providers configuration
   */
  public static <E> Map<String, Class<? extends E>>
    loadProvidersConfiguration(Class<E> base, String confPropertiesName)
  {
    HashMap<String, Class<? extends E>> providers;
    providers = new HashMap<String, Class<? extends E>>();
    InputStream in;

    /* Try to read built-in provider configuration */
    in = base.getResourceAsStream(confPropertiesName);
    if (in != null) {
      providers.putAll(readProvidersConfiguration(base, in, "built-in"));
    }
    else System.err.format("Built-in configuration not found for %s\n", base.getName());

    /* Try to read external provider configuration */
    in = base.getResourceAsStream("/" + confPropertiesName);
    if (in != null) {
      providers.putAll(readProvidersConfiguration(base, in, "external"));
    }

    return providers;
  }

  /**
   * Creates a new instance of the specified provider looking for the associated class in the
   * configuration map.
   *
   * @param providerMap The configuration map of the providers
   * @param provider The provider to instantiate
   * @param paramsClass The signature of the constructor to invoke
   * @param paramValues The parameters to pass to the constructor
   * @return An instance of the specific provider
   * @exception InstantiationException If any error occurs while instantiating. If the constructor
   * throw an exception then the cause of this will be an InvocationTargetException.
   * @exception IllegalArgumentException If no configuration is found for the specified provider
   */
  public static <E> E newInstance(Map<String, Class<? extends E>> providerMap,
                                  String provider, Class[] paramsClass, Object[] paramValues)
    throws InstantiationException, IllegalArgumentException
  {
    Class<? extends E> clazz = providerMap.get(provider);

    if (clazz == null)
      throw new IllegalArgumentException("No configuration for provider: " + provider);

    E instance = null;
    try {
      Constructor<? extends E> cloudcastConstructor;
      cloudcastConstructor = clazz.getConstructor(paramsClass);

      instance = cloudcastConstructor.newInstance(paramValues);
    } catch (Exception e) {
      InstantiationException ex;
      ex = new InstantiationException("Error instantiating provider " + provider);
      ex.initCause(e);
      throw ex;
    }
    return instance;
  }
}

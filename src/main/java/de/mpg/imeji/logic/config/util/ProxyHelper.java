package de.mpg.imeji.logic.config.util;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProxyHelper {
  private static String proxyHost = null;
  private static String proxyPort = null;
  private static String nonProxyHosts = null;
  private static Pattern nonProxyPattern = null;
  private static boolean flag = false;

  private ProxyHelper() {}

  /**
   * check if proxy has to get used for given url. If yes, set ProxyHost in httpClient
   *
   * @param url url
   * @throws Exception
   */
  public static RequestConfig getRequestConfigProxy(final RequestConfig requestConfig, final String url) {
    getProxyProperties();
    if (proxyHost != null) {

      if (findUrlInNonProxyHosts(url)) {
        return RequestConfig.copy(requestConfig).build();
      } else {
        return RequestConfig.copy(requestConfig).setProxy(new HttpHost(proxyHost, Integer.valueOf(proxyPort))).build();
        //hc.setProxy(proxyHost, Integer.valueOf(proxyPort));
      }
    }
    return requestConfig;
  }

  /**
   * Returns <code>java.net.Proxy</code> class for <code>java.net.URL.openConnection</code> creation
   *
   * @param url url
   * @throws Exception
   */
  private static Proxy getProxy(final String url) {
    Proxy proxy = Proxy.NO_PROXY;
    getProxyProperties();
    if (proxyHost != null) {
      if (!findUrlInNonProxyHosts(url)) {
        final SocketAddress proxyAddress = new InetSocketAddress(proxyHost, Integer.valueOf(proxyPort));
        proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
      }
    }
    return proxy;
  }

  /**
   * Wrapper for executeMethod with Proxy
   *
   * @param client, methopd
   * @throws IOException
   * @throws HttpException
   */
  public static CloseableHttpResponse executeMethod(CloseableHttpClient client, HttpRequestBase method) throws IOException {
    method.setConfig(getRequestConfigProxy(method.getConfig(), method.getURI().toString()));
    return client.execute(method);
  }

  /**
   * Returns <code>java.net.URLConnection</code> with the Proxy settings creation
   *
   * @param url url
   * @throws IOException
   * @throws Exception
   * @return URLConnection
   */
  public static URLConnection openConnection(final URL url) throws IOException {
    return url.openConnection(getProxy(url.toString()));
  }

  /**
   * Read proxy properties, set nonProxyPattern
   */
  private static void getProxyProperties() {
    if (flag) {
      return;
    }
    try {
      proxyHost = PropertyReader.getProperty("http.proxyHost");
      proxyPort = PropertyReader.getProperty("http.proxyPort");
      nonProxyHosts = PropertyReader.getProperty("http.nonProxyHosts");
      if (nonProxyHosts != null && !nonProxyHosts.trim().equals("")) {
        String nph = nonProxyHosts;
        nph = nph.replaceAll("\\.", "\\\\.").replaceAll("\\*", "").replaceAll("\\?", "\\\\?");
        nonProxyPattern = Pattern.compile(nph);
      }
      flag = true;
    } catch (final Exception e) {
      throw new RuntimeException("Cannot read proxy configuration:", e);
    }
  }

  /**
   * Find <code>url</code> in the list of the nonProxyHosts
   *
   * @param url
   * @return <code>true</code> if <code>url</code> is found, <code>false</code> otherwise
   */
  private static boolean findUrlInNonProxyHosts(String url) {
    getProxyProperties();
    if (nonProxyPattern != null) {
      final Matcher nonProxyMatcher = nonProxyPattern.matcher(url);
      return nonProxyMatcher.find();
    } else {
      return false;
    }
  }
}

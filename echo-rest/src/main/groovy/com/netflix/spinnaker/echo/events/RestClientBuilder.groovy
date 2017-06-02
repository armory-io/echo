package com.netflix.spinnaker.echo.events

import com.squareup.okhttp.OkHttpClient
import org.springframework.stereotype.Component
import retrofit.client.Client
import retrofit.client.OkClient

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Component
class RestClientBuilder {
  Client buildClient(Boolean insecure) {
    if (insecure == true) {
      return getInsecureClient()
    } else {
      new OkClient()
    }
  }

  Client getInsecureClient() {
    // Create a trust manager that does not validate certificate chains
    def trustAllCerts = [
      checkClientTrusted: { chain, authType ->  },
      checkServerTrusted: { chain, authType ->  },
      getAcceptedIssuers: { null }
    ]

    def nullHostnameVerifier = [
      verify: { hostname, session -> true }
    ]
    SSLContext sc = SSLContext.getInstance("SSL")
    sc.init(null, [trustAllCerts as X509TrustManager] as TrustManager[], null)

    SSLSocketFactory sslSocketFactory = sc.getSocketFactory()
    OkHttpClient okHttpClient = new OkHttpClient()
    okHttpClient.setSslSocketFactory(sslSocketFactory)
    okHttpClient.setHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
    return new OkClient(okHttpClient)
  }
}

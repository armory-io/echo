package com.netflix.spinnaker.echo.events

import spock.lang.Specification
import spock.lang.Subject

/**
 * Created by isaacmosquera on 6/2/17.
 */
class RestClientBuilderSpec extends Specification{

  @Subject
  RestClientBuilder clientBuilder = new RestClientBuilder()

  void 'returns insecure client'() {
    given:
      def client = clientBuilder.buildClient(true)
  }
}

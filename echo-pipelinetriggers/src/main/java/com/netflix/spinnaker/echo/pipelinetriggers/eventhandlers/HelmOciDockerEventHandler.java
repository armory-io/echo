/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.echo.pipelinetriggers.eventhandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.echo.model.Trigger;
import com.netflix.spinnaker.echo.model.trigger.HelmOciDockerEvent;
import com.netflix.spinnaker.fiat.shared.FiatPermissionEvaluator;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of TriggerEventHandler for events of type {@link HelmOciDockerEvent}, which occur
 * when a new container is pushed to a docker registry.
 */
@Component
public class HelmOciDockerEventHandler extends BaseTriggerEventHandler<HelmOciDockerEvent> {
  private static final String TRIGGER_TYPE = "helm-docker";
  private static final List<String> supportedTriggerTypes = Collections.singletonList(TRIGGER_TYPE);

  @Autowired
  public HelmOciDockerEventHandler(
      Registry registry,
      ObjectMapper objectMapper,
      FiatPermissionEvaluator fiatPermissionEvaluator) {
    super(registry, objectMapper, fiatPermissionEvaluator);
  }

  @Override
  public List<String> supportedTriggerTypes() {
    return supportedTriggerTypes;
  }

  @Override
  public boolean handleEventType(String eventType) {
    return eventType.equalsIgnoreCase(HelmOciDockerEvent.TYPE);
  }

  @Override
  public Class<HelmOciDockerEvent> getEventType() {
    return HelmOciDockerEvent.class;
  }

  @Override
  public boolean isSuccessfulTriggerEvent(HelmOciDockerEvent helmOciDockerEvent) {
    // The event should always report a tag
    String tag = helmOciDockerEvent.getContent().getTag();
    return tag != null && !tag.isEmpty();
  }

  protected List<Artifact> getArtifactsFromEvent(
      HelmOciDockerEvent helmOciDockerEvent, Trigger trigger) {
    HelmOciDockerEvent.Content content = helmOciDockerEvent.getContent();

    String name = content.getRegistry() + "/" + content.getRepository();
    String reference = name + ":" + content.getTag();
    return Collections.singletonList(
        Artifact.builder()
            .type("helm/image")
            .name(name)
            .version(content.getTag())
            .reference(reference)
            .build());
  }

  @Override
  protected Function<Trigger, Trigger> buildTrigger(HelmOciDockerEvent helmOciDockerEvent) {
    return trigger ->
        trigger
            .atTag(
                helmOciDockerEvent.getContent().getTag(),
                helmOciDockerEvent.getContent().getDigest())
            .withEventId(helmOciDockerEvent.getEventId());
  }

  @Override
  protected boolean isValidTrigger(Trigger trigger) {
    return trigger.isEnabled()
        && ((TRIGGER_TYPE.equals(trigger.getType())
            && trigger.getAccount() != null
            && trigger.getRepository() != null));
  }

  private boolean matchTags(String suppliedTag, String incomingTag) {
    try {
      // use matches to handle regex or basic string compare
      return incomingTag.matches(suppliedTag);
    } catch (PatternSyntaxException e) {
      return false;
    }
  }

  @Override
  protected Predicate<Trigger> matchTriggerFor(HelmOciDockerEvent helmOciDockerEvent) {
    return trigger -> isMatchingTrigger(helmOciDockerEvent, trigger);
  }

  private boolean isMatchingTrigger(HelmOciDockerEvent helmOciDockerEvent, Trigger trigger) {
    String account = helmOciDockerEvent.getContent().getAccount();
    String repository = helmOciDockerEvent.getContent().getRepository();
    String eventTag = helmOciDockerEvent.getContent().getTag();
    String triggerTagPattern = null;
    if (StringUtils.isNotBlank(trigger.getTag())) {
      triggerTagPattern = trigger.getTag().trim();
    }
    return trigger.getType().equals(TRIGGER_TYPE)
        && trigger.getRepository().equals(repository)
        && trigger.getAccount().equals(account)
        && ((triggerTagPattern == null && !eventTag.equals("latest"))
            || triggerTagPattern != null && matchTags(triggerTagPattern, eventTag));
  }
}

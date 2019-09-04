/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.contract.core;

import io.yggdrash.contract.core.channel.ContractEventType;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContractEventSet implements Serializable {

    public Set<ContractEvent> events = new HashSet<>();

    public ContractEventSet() {
    }

    public ContractEventSet(ContractEvent event) {
        events.add(event);
    }

    public ContractEventSet(List<ContractEvent> event) {
        events.addAll(event);
    }

    public void addEvents(ContractEvent event) {
        events.add(event);
    }

    public void removeExpireEvent() {
        List<ContractEvent> expireEvents = events.stream()
                .filter(event -> event.getType().equals(ContractEventType.EXPIRED))
                .collect(Collectors.toList());

        if (!expireEvents.isEmpty()) {
            events.removeAll(expireEvents);
        }
    }

    public Set<ContractEvent> getEvents() {
        return events;
    }

}

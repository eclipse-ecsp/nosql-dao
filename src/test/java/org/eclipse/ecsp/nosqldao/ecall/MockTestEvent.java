/*
 *
 *
 *   *******************************************************************************
 *
 *     Copyright (c) 2023-24 Harman International
 *
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *
 *     you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at
 *
 *
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 *     Unless required by applicable law or agreed to in writing, software
 *
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *     See the License for the specific language governing permissions and
 *
 *     limitations under the License.
 *
 *
 *
 *     SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 *
 */

package org.eclipse.ecsp.nosqldao.ecall;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import org.eclipse.ecsp.entities.AbstractIgniteEvent;
import org.eclipse.ecsp.entities.IgniteEvent;

import java.util.List;

/** MockTestEvent class. */
@Entity()
@Indexes(@Index(fields = {@Field(value = "vehicleId"), @Field(value = "sourceDeviceId")}))
public class MockTestEvent extends AbstractIgniteEvent {

    private static final long serialVersionUID = -1785142819686025246L;
    @Id
    private String id;
    private long hits;
    private long dunks;

    public String getId() {
        return id;
    }

    @Id
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public List<IgniteEvent> getNestedEvents() {
        return null;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getDunks() {
        return dunks;
    }

    public void setDunks(long dunks) {
        this.dunks = dunks;
    }

}
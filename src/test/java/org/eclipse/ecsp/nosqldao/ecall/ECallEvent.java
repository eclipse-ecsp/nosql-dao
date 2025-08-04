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
import dev.morphia.annotations.Property;
import org.eclipse.ecsp.entities.AbstractIgniteEvent;
import org.eclipse.ecsp.entities.AuditableIgniteEntity;
import org.eclipse.ecsp.entities.IgniteEvent;
import org.eclipse.ecsp.nosqldao.mongodb.BytesBuffer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ECallEvent entity class.
 */
@Entity("ecallEvents")
@Indexes(@Index(fields = {@Field(value = "vehicleId"), @Field(value = "sourceDeviceId")}))
public class ECallEvent extends AbstractIgniteEvent implements AuditableIgniteEntity {

    private static final long serialVersionUID = -1785142819686025246L;
    @Id
    private String ecallId;
    private long hits;
    private long dunks;
    @Property("testCounter")
    private long counter;
    private BytesBuffer bytesBuffer;
    private List<String> listAttr1 = new ArrayList<String>();
    private List<String> listAttr2 = new ArrayList<String>();
    private Set<String> setAttr1 = new HashSet<String>();
    private Set<String> setAttr2 = new HashSet<String>();
    private double doubleData;
    private short shortData;
    private float floatData;
    private int intData;
    private LocalDateTime lastUpdatedTime;
    private LocalDate localDate;
    private LocalTime localTime;
    private List<AuthUsers> authorizedUsers;
    private TestEntity entity;
    private Map<String, Map<String, String>> customParams;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public double getDoubleData() {
        return doubleData;
    }

    public void setDoubleData(double doubleData) {
        this.doubleData = doubleData;
    }

    public short getShortData() {
        return shortData;
    }

    public void setShortData(short shortData) {
        this.shortData = shortData;
    }

    public float getFloatData() {
        return floatData;
    }

    public void setFloatData(float floatData) {
        this.floatData = floatData;
    }

    public int getIntData() {
        return intData;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public Map<String, Map<String, String>> getCustomParams() {
        return this.customParams;
    }

    public void setCustomParams(Map<String, Map<String, String>> customParams) {
        this.customParams = customParams;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public TestEntity getEntity() {
        return this.entity;
    }

    public void setEntity(TestEntity entity) {
        this.entity = entity;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public List<AuthUsers> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public void setAuthorizedUsers(List<AuthUsers> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public String getEcallId() {
        return ecallId;
    }

    @Id
    public void setEcallId(String ecallId) {
        this.ecallId = ecallId;
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

    public List<String> getListAttr1() {
        return listAttr1;
    }

    public void setListAttr1(List<String> strings) {
        this.listAttr1 = strings;
    }

    public Set<String> getSetAttr1() {
        return setAttr1;
    }

    public void setSetAttr1(Set<String> integers) {
        this.setAttr1 = integers;
    }

    public List<String> getListAttr2() {
        return listAttr2;
    }

    public void setListAttr2(List<String> listAttr2) {
        this.listAttr2 = listAttr2;
    }

    public Set<String> getSetAttr2() {
        return setAttr2;
    }

    public void setSetAttr2(Set<String> setAttr2) {
        this.setAttr2 = setAttr2;
    }

    public BytesBuffer getBytesBuffer() {
        return bytesBuffer;
    }

    public void setBytesBuffer(BytesBuffer bytesBuffer) {
        this.bytesBuffer = bytesBuffer;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    @Override
    public LocalDateTime getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public void setLastUpdatedTime(LocalDateTime lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;

    }

    @Override
    public String toString() {
        return "AbstractIgniteEvent [TestEntity = " + this.entity + ", version=" + version + ", timestamp="
                + timestamp + ", eventData=" + eventData + ", requestId=" + requestId
                + ", sourceDeviceId=" + sourceDeviceId + ", vehicleId=" + vehicleId + "]";
    }

    /**
     * AuthUsers entity class.
     */
    @Entity
    public static class AuthUsers {
        private String userId;

        private String role;
        private String status;
        private LocalDateTime createdOn;
        private LocalDateTime updatedOn;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public void setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
        }

    }
}
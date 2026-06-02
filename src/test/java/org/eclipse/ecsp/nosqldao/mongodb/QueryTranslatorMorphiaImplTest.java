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

package org.eclipse.ecsp.nosqldao.mongodb;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.ecall.ECallEvent;
import org.eclipse.ecsp.nosqldao.spring.config.IgniteDAOMongoConfigWithProps;
import org.eclipse.ecsp.nosqldao.utils.EmbeddedMongoDB;
import org.eclipse.ecsp.nosqldao.utils.NumericConstants;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Testing QueryTranslatorMorphiaImpl.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IgniteDAOMongoConfigWithProps.class })
@TestPropertySource("/ignite-dao.properties")
public class QueryTranslatorMorphiaImplTest {

    @ClassRule
    public static final EmbeddedMongoDB MONGO_SERVER = new EmbeddedMongoDB();

    private static final String VEHICLEID = "vehicleId";
    private static final String SOURCEDEVICEID = "sourceDeviceId";

    @Autowired
    private Datastore datastore;

    private String customCollection = "customColl";

    @Test
    public void testSigleCriteriaQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{" + VEHICLEID
                + "=Vehicle1}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.of(customCollection));
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testSingleCriteriaGroupWithAndQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$and=[Document{{" + VEHICLEID
                + "=Vehicle1}}, " + "Document{{" + SOURCEDEVICEID + "=Device1}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).and(c2);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testSingleCriteriaGroupWithOrQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$or=[Document{{" + VEHICLEID
                + "=Vehicle1}}," + " Document{{" + SOURCEDEVICEID + "=Device1}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).or(c2);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testSingleCriteriaGroupWithAndOrQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$or=[Document{{$and=[Document{{" + VEHICLEID
                + "=Vehicle1}}, Document{{" + SOURCEDEVICEID + "=Device1}}]}}, "
                + "Document{{$and=[Document{{eventId=Ecall}}, Document{{version=Document{{$lt=V1}}}}]}}, "
                + "Document{{timestamp=Document{{$gt=100}}}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteria c3 = new IgniteCriteria("eventId", Operator.EQ, "Ecall");
        IgniteCriteria c4 = new IgniteCriteria("version", Operator.LT, "V1");
        IgniteCriteria c5 = new IgniteCriteria("timestamp", Operator.GT, "100");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).and(c2).or(c3).and(c4).or(c5);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        // queryMorphia.init(daoProperties);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testMultipleCriteriaGroupWithOrQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$or=[Document{{$and=[Document{{" + VEHICLEID
                + "=Vehicle1}}, Document{{"
                + SOURCEDEVICEID + "=Device1}}]}},"
                + " Document{{$and=[Document{{" + VEHICLEID + "=Vehicle2}}, Document{{"
                + SOURCEDEVICEID + "=Device2}}]}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).and(c2);

        IgniteCriteria c3 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle2");
        IgniteCriteria c4 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device2");
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c4);

        IgniteQuery igQuery = new IgniteQuery(cg1).or(cg2);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testMultipleCriteriaGroupWithAndQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$and=[Document{{$and=[Document{{"
                + VEHICLEID + "=Vehicle1}}, Document{{"
                + SOURCEDEVICEID + "=Device1}}]}}, "
                + "Document{{$and=[Document{{" + VEHICLEID + "=Vehicle2}}, Document{{"
                + SOURCEDEVICEID + "=Device2}}]}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).and(c2);

        IgniteCriteria c3 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle2");
        IgniteCriteria c4 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device2");
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c4);

        IgniteQuery igQuery = new IgniteQuery(cg1).and(cg2);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testMultipleCriteriaGroupWithOrAndQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$or=["
                + "Document{{$and=[Document{{$and=[Document{{"
                + VEHICLEID + "=Vehicle1}},"
                + " Document{{" + SOURCEDEVICEID + "=Device1}}]}}, Document{{$and=[Document{{"
                + VEHICLEID + "=Vehicle2}},"
                + " Document{{" + SOURCEDEVICEID + "=Device2}}]}}]}}, Document{{$or=[Document{{version=V1}}, "
                + "Document{{timestamp=100}}]}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteria c2 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1).and(c2);

        IgniteCriteria c3 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle2");
        IgniteCriteria c4 = new IgniteCriteria(SOURCEDEVICEID, Operator.EQ, "Device2");
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c4);

        IgniteCriteria c5 = new IgniteCriteria("version", Operator.EQ, "V1");
        IgniteCriteria c6 = new IgniteCriteria("timestamp", Operator.EQ, "100");
        IgniteCriteriaGroup cg3 = new IgniteCriteriaGroup(c5).or(c6);

        IgniteQuery igQuery = new IgniteQuery(cg1).and(cg2).or(cg3);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testNotEqualsCriteriaQuery() {
        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{" + VEHICLEID
                + "=Document{{$ne=Vehicle1}}}}]";
        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.NEQ, "Vehicle1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);
        IgniteQuery igQuery = new IgniteQuery(cg1);
        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> query = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, query.toString());
    }

    @Test
    public void testElementMatchQuery() {
        IgniteQuery igQuery = new IgniteQuery();
        IgniteQuery elementMatchQuery = new IgniteQuery();

        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$and=[Document{{"
                + VEHICLEID + "=Vehicle1}}, "
                + "Document{{" + SOURCEDEVICEID + "=Document{{$elemMatch=Document{{$or=["
                + "Document{{$and=[Document{{version=102}}, "
                + "Document{{timestamp=Document{{$gt=100}}}}]}}]}}}}}}]}}]";

        IgniteCriteria c1 = new IgniteCriteria(VEHICLEID, Operator.EQ, "Vehicle1");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);

        IgniteCriteria c2 = new IgniteCriteria("timestamp", Operator.GT, NumericConstants.HUNDRED);
        IgniteCriteria c3 = new IgniteCriteria("version", Operator.EQ, NumericConstants.HUNDRED_TWO);

        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c2);
        elementMatchQuery.and(cg2);

        IgniteCriteria elementMatchCriteria = new IgniteCriteria(SOURCEDEVICEID,
                Operator.ELEMENT_MATCH, elementMatchQuery);
        IgniteCriteriaGroup elementMatchCriteriaGroup = new IgniteCriteriaGroup(elementMatchCriteria);
        igQuery.and(cg1).and(elementMatchCriteriaGroup);

        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> outputQuery = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, outputQuery.toString());
    }

    @Test
    public void testElementMatch() {
        IgniteQuery igQuery = new IgniteQuery();
        IgniteQuery elementMatchQuery = new IgniteQuery();
        final String ZIPCODE = "zipcode";
        final String STUDENTS = "students";
        final String SCHOOL = "school";
        final String AGE = "age";

        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$and=[Document{{zipcode=63109}},"
                + " Document{{students=Document{{$elemMatch=Document{{$or=[Document{{$and=[Document{{school=102}},"
                + " Document{{age=Document{{$gt=7}}}}]}}]}}}}}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(ZIPCODE, Operator.EQ, "63109");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);

        IgniteCriteria c2 = new IgniteCriteria(AGE, Operator.GT, NumericConstants.SEVEN);
        IgniteCriteria c3 = new IgniteCriteria(SCHOOL, Operator.EQ, NumericConstants.HUNDRED_TWO);

        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c2);
        elementMatchQuery.and(cg2);

        IgniteCriteria elementMatchCriteria = new IgniteCriteria(STUDENTS, Operator.ELEMENT_MATCH, elementMatchQuery);
        IgniteCriteriaGroup elementMatchCriteriaGroup = new IgniteCriteriaGroup(elementMatchCriteria);
        igQuery.and(cg1).and(elementMatchCriteriaGroup);

        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> outputQuery = queryMorphia.translate(igQuery, Optional.empty());
        assertEquals("Actual query does not match with expected", expected, outputQuery.toString());
    }

    @Test
    public void testElementMatchWithMultipleOp() {
        IgniteQuery igQuery = new IgniteQuery();
        IgniteQuery elementMatchQuery = new IgniteQuery();
        final String ZIPCODE = "zipcode";
        final String STUDENTS = "students";
        final String SCHOOL = "school";
        final String AGE = "age";
        final String SUBJECT = "subject";

        String expected = "MorphiaQuery[clazz=ECallEvent, query=Document{{$and=[Document{{zipcode=63109}}, "
                + "Document{{students=Document{{$elemMatch=Document{{$or=[Document{{$and=["
                + "Document{{school=Document{{$lte=102}}}}, "
                + "Document{{age=Document{{$gte=7}}}}]}}, "
                + "Document{{$and=[Document{{subject=Document{{$in=[Physics, Biology]}}}}, "
                + "Document{{subject=Document{{$nin=[Chemistry]}}}}]}}]}}}}}}]}}]";
        IgniteCriteria c1 = new IgniteCriteria(ZIPCODE, Operator.EQ, "63109");
        IgniteCriteriaGroup cg1 = new IgniteCriteriaGroup(c1);

        IgniteCriteria c2 = new IgniteCriteria(AGE, Operator.GTE, NumericConstants.SEVEN);
        IgniteCriteria c3 = new IgniteCriteria(SCHOOL, Operator.LTE, NumericConstants.HUNDRED_TWO);
        IgniteCriteriaGroup cg2 = new IgniteCriteriaGroup(c3).and(c2);

        IgniteCriteria c4 = new IgniteCriteria(SUBJECT, Operator.IN, Arrays.asList("Physics", "Biology"));
        IgniteCriteria c5 = new IgniteCriteria(SUBJECT, Operator.NOT_IN, Arrays.asList("Chemistry"));
        IgniteCriteriaGroup cg3 = new IgniteCriteriaGroup(c4).and(c5);

        elementMatchQuery.and(cg2).and(cg3);

        IgniteCriteria elementMatchCriteria = new IgniteCriteria(STUDENTS, Operator.ELEMENT_MATCH, elementMatchQuery);
        IgniteCriteriaGroup elementMatchCriteriaGroup = new IgniteCriteriaGroup(elementMatchCriteria);
        igQuery.and(cg1).and(elementMatchCriteriaGroup);

        QueryTranslatorMorphiaImpl<ECallEvent> queryMorphia = new QueryTranslatorMorphiaImpl<>(
                datastore, ECallEvent.class);
        Query<ECallEvent> outputQuery = queryMorphia.translate(igQuery, Optional.empty());

        assertEquals("Actual query does not match with expected", expected, outputQuery.toString());
    }

}

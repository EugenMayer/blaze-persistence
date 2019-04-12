/*
 * Copyright 2014 - 2019 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.testsuite;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.base.jpa.category.NoHibernate42;
import com.blazebit.persistence.testsuite.base.jpa.category.NoHibernate43;
import com.blazebit.persistence.testsuite.base.jpa.category.NoHibernate50;
import com.blazebit.persistence.testsuite.entity.IntIdEntity;
import com.blazebit.persistence.testsuite.entity.PolymorphicBase;
import com.blazebit.persistence.testsuite.entity.PolymorphicSub1;
import com.blazebit.persistence.testsuite.entity.PolymorphicSub2;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Moritz Becker
 * @since 1.4.0
 */
// NOTE: Requires entity joins which are supported since Hibernate 5.1, Datanucleus 5 and latest Eclipselink
@Category({ NoHibernate42.class, NoHibernate43.class, NoHibernate50.class })
public class TreatedEntityJoinTest extends AbstractCoreTest {

    private PolymorphicSub1 root2;

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[]{
                IntIdEntity.class,
                PolymorphicBase.class,
                PolymorphicSub1.class,
                PolymorphicSub2.class
        };
    }

    @Override
    protected void setUpOnce() {
        cleanDatabase();
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                PolymorphicSub1 root1 = new PolymorphicSub1();
                PolymorphicSub1 parent1 = new PolymorphicSub1();
                root2 = new PolymorphicSub1();
                PolymorphicSub2 parent2 = new PolymorphicSub2();

                root1.setSub1Value(1);
                root1.setParent(parent1);
                root2.setSub1Value(2);
                root2.setParent(parent2);

                em.persist(parent1);
                em.persist(parent2);
                em.persist(root1);
                em.persist(root2);
            }
        });
    }

    @Before
    public void load() {
        root2 = em.createQuery("SELECT e FROM PolymorphicSub1 e WHERE e.sub1Value = 2", PolymorphicSub1.class).getSingleResult();
    }

    @Test
    public void test1() {
        CriteriaBuilder<PolymorphicSub1> cb = cbf.create(em, PolymorphicSub1.class, "root")
                .innerJoinOn("TREAT(parent AS PolymorphicSub2)", PolymorphicBase.class, "parentAlias")
                    .on("parentAlias.id").eqExpression("TREAT(root.parent AS PolymorphicSub2).id")
                .end();

        String expectedQuery = "SELECT root FROM PolymorphicSub1 root " +
                "LEFT JOIN root.parent parent_1 " +
                "JOIN PolymorphicBase parentAlias" + onClause("(TYPE(parent_1) = " + PolymorphicSub2.class.getSimpleName() + " AND parentAlias.id = " + treatRoot("parent_1", PolymorphicSub2.class, "id") + ")");
        assertEquals(expectedQuery, cb.getQueryString());
        List<PolymorphicSub1> result = cb.getResultList();
        assertEquals(1, result.size());
        assertEquals(root2.getId(), result.get(0).getId());
    }

    @Test
    public void test2() {
        CriteriaBuilder<PolymorphicSub1> cb = cbf.create(em, PolymorphicSub1.class, "root")
                .innerJoinOn("TREAT(parent AS PolymorphicSub2)", PolymorphicBase.class, "parent")
                    .on("parent.id").eqExpression("TREAT(root.parent AS PolymorphicSub2).id")
                .end();

        String expectedQuery = "SELECT root FROM PolymorphicSub1 root " +
                "LEFT JOIN root.parent parent_1 " +
                "JOIN PolymorphicBase parent" + onClause("(TYPE(parent_1) = " + PolymorphicSub2.class.getSimpleName() + " AND parent.id = " + treatRoot("parent_1", PolymorphicSub2.class, "id") + ")");
        assertEquals(expectedQuery, cb.getQueryString());
        List<PolymorphicSub1> result = cb.getResultList();
        assertEquals(1, result.size());
        assertEquals(root2.getId(), result.get(0).getId());
    }
}

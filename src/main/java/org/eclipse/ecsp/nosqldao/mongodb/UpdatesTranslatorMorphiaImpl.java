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

import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import org.eclipse.ecsp.nosqldao.Updates;
import org.eclipse.ecsp.nosqldao.Updates.DecOp;
import org.eclipse.ecsp.nosqldao.Updates.FieldSetOp;
import org.eclipse.ecsp.nosqldao.Updates.IncOp;
import org.eclipse.ecsp.nosqldao.Updates.PushMultiOp;
import org.eclipse.ecsp.nosqldao.Updates.PushOp;
import org.eclipse.ecsp.nosqldao.Updates.RemoveOp;
import org.eclipse.ecsp.nosqldao.Updates.SetMultiOp;
import org.eclipse.ecsp.nosqldao.Updates.SetOp;
import org.eclipse.ecsp.nosqldao.Updates.UnsetOp;
import org.eclipse.ecsp.nosqldao.UpdatesTranslator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This Class provides methods for UpdatesTranslatorMorphia implementation extending IgniteEntity.
 */
public class UpdatesTranslatorMorphiaImpl implements UpdatesTranslator<List<UpdateOperator>> {

    /**
     * Translate list.
     *
     * @param updates        the updates
     * @param collectionName the collection name
     * @return the list
     */
    @Override
    public List<UpdateOperator> translate(Updates updates, Optional<String> collectionName) {
        List<UpdateOperator> updateOperations = new ArrayList<>();
        populateOperations(updateOperations, updates);
        return updateOperations;
    }

    /**
     * Populate operations.
     *
     * @param updateOperations the update operations
     * @param updates          the updates
     */
    private void populateOperations(List<UpdateOperator> updateOperations, Updates updates) {
        updates.traverse(new Updates.UpdateOpVisitor() {
            @Override
            public void visit(DecOp op) {
                if (op.getBy() > 0) {
                    updateOperations.add(UpdateOperators.dec(op.getField(), op.getBy()));
                } else {
                    updateOperations.add(UpdateOperators.dec(op.getField()));
                }
            }

            @Override
            public void visit(IncOp op) {
                if (op.getBy() > 0) {
                    updateOperations.add(UpdateOperators.inc(op.getField(), op.getBy()));
                } else {
                    updateOperations.add(UpdateOperators.inc(op.getField()));
                }
            }

            @Override
            public void visit(SetOp op) {
                updateOperations.add(UpdateOperators.addToSet(op.getField(), op.getVal()));
            }

            @Override
            public void visit(PushOp op) {
                if (op.getVal() instanceof Collection) {
                    updateOperations.add(UpdateOperators.push(op.getField(),
                            new ArrayList<>((Collection<?>) op.getVal())));
                } else {
                    updateOperations.add(UpdateOperators.push(op.getField(), op.getVal()));
                }
            }

            @Override
            public void visit(UnsetOp op) {
                updateOperations.add(UpdateOperators.unset(op.getField()));
            }

            @Override
            public void visit(FieldSetOp op) {
                updateOperations.add(UpdateOperators.set(op.getField(), op.getVal()));
            }

            @Override
            public void visit(PushMultiOp op) {
                updateOperations.add(UpdateOperators.push(op.getField(), op.getValues()));
            }

            @Override
            public void visit(@SuppressWarnings("rawtypes") SetMultiOp op) {
                updateOperations.add(UpdateOperators.addToSet(op.getField(), op.getValues()));
            }

            @Override
            public void visit(RemoveOp op) {
                if (op.getVal() instanceof Collection) {
                    updateOperations.add(UpdateOperators.pullAll(op.getField(),
                            new ArrayList<>((Collection<?>) op.getVal())));
                } else {
                    String[] fieldArray = op.getField().split("\\.");
                    updateOperations.add(UpdateOperators.pull(fieldArray[0], Filters.eq(fieldArray[1], op.getVal())));
                }
            }
        });
    }
}

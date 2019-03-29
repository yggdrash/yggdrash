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

package io.yggdrash.core.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
TODO TransactionQuery provides a parser for a custom query format.

    blockNum>100 AND owner=Brian

    - tag : blockNum, owner
    - operator : >, =
    - value : 100, brian

There is no specification for the query currently.
*/
public class TransactionQuery {

    private BranchId branchId;
    private List<Condition> conditions;
    private Map<String, String> tagValueSet;
    private Map<Integer, Map<String, String>> query; // i.e. {OpEqual : {BlockNum : 10}}

    TransactionQuery setBranchId(BranchId branchId) {
        this.branchId = branchId;
        return this;
    }

    TransactionQuery setTagValue(String tag, String value) {
        this.tagValueSet = new HashMap<>();
        this.tagValueSet.put(tag, value);
        return this;
    }

    TransactionQuery setTagValueSet(Map<String, String> tagValueSet) {
        this.tagValueSet = tagValueSet;
        return this;
    }

    TransactionQuery setOpCode(Integer opCode) {
        this.query = new HashMap<>();
        this.query.put(opCode, tagValueSet);
        this.conditions = new ArrayList<>();
        setConditions();
        return this;
    }

    TransactionQuery() {
    }

    BranchId getBranchId() {
        return branchId;
    }

    Map<Integer, Map<String, String>> getQuery() {
        return query;
    }

    List<Condition> getConditions() {
        return conditions;
    }

    int opCount() {
        return query.keySet().size();
    }

    int tagCount() {
        int num = 0;
        for (Integer opCode : query.keySet()) {
            num += query.get(opCode).keySet().size();
        }
        return num;
    }

    int tagCountOf(int opCode) {
        return query.get(opCode).keySet().size();
    }

    boolean containTag(String tag) {
        return query.keySet().stream().findFirst().filter(op -> query.get(op).keySet().contains(tag)).isPresent();
    }

    private void setConditions() {

        for (Integer opCode : query.keySet()) {
            for (String tag : query.get(opCode).keySet()) {
                String value = query.get(opCode).get(tag);
                addCondition(new Condition(tag, opCode, value));
            }
        }
    }

    private void addCondition(Condition condition) {
        conditions.add(condition);
    }

    public class Condition {
        String tag;
        int operator; // operator defines some kind of relation between tag and operand(equality, etc.)
        String value;

        Condition(String tag, int opCode, String value) {
            this.tag = tag;
            this.operator = opCode;
            this.value = value;
        }

        public String getTag() {
            return tag;
        }

        public int getOperator() {
            return operator;
        }

        public String getValue() {
            return value;
        }

        boolean conatinsTag(String t) {
            return tag.equals(t);
        }

        void operand(String value) {

        }
    }

    public interface Operator {
        int OpLessEqual = 0;    // "<="
        int OpGreaterEqual = 1; // ">="
        int OpLess = 2;         // "<"
        int OpGreater = 3;      // ">"
        int OpEqaul = 4;        // "="
        int OpContains = 5;     // "CONTAINS"; used to check if a string contains a certain sub string
    }
}

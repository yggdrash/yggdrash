/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract.methods;

public class ContractMethodInfo {
    private String name;
    private Class<?> outputType;
    private Class<?>[] inputType;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getOutputType() {
        return outputType;
    }

    public void setOutputType(Class<?> outputType) {
        this.outputType = outputType;
    }

    public Class<?>[] getInputType() {
        return inputType;
    }

    public void setInpiutType(Class<?>[] inpitType) {
        this.inputType = inpitType;
    }
}

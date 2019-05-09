
package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.standard.CoinStandard;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.BranchUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.Genesis;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;


public class StemContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(StemContract.class);
    //private ServiceRegistration registration;

    // Get other Service
    private CoinStandard asset;


    @Override
    public void start(BundleContext context) {
        log.info("Start stem contract");

        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "Stem");
        context.registerService(StemService.class.getName(), new StemService(), props);
        //Register our service in the bundle context using the name.
        //registration = context.registerService(StemService.class.getName(), new StemService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("Stop stem contract");
        //TODO Why don't unregister the service?
        //registration.unregister();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        // get YEED contract in this
        //
    }

    public void setAsset(CoinStandard coinStandard) {
        this.asset = coinStandard;
    }

    public static class StemService implements Contract {
        private static final Logger log = LoggerFactory.getLogger(StemContract.class);

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;


        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @Genesis
        @InvokeTransaction // TODO remove InvokeTransaction
        public TransactionReceipt init(JsonObject param) {
            //txReceipt = create(param);
            log.info("[StemContract | genesis] SUCCESS! param => {}", param);
            return txReceipt;
        }

        /**
         * Returns the id of a registered branch
         *
         * @param params branch   : The branch.json to register on the stem
         */
        @InvokeTransaction
        public void create(JsonObject params) {
            // TODO store branch
            // Validate branch spec
            // params

            JsonObject branch = params.getAsJsonObject("branch");


            // branch verify
            if (!branchVerify(branch)) {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
                this.txReceipt.addLog("Branch had not verified");
                return;
            }

            // calculation branch Id
            String branchId = HexUtil.toHexString(BranchUtil.branchIdGenerator(branch));
            log.debug("branchId : {}", branchId);
            // prefix Branch_id


            boolean save = saveBranch(branchId, branch);

            // get Validator
            // TODO add contract governance flag



            // get meta information
            JsonObject branchCopy = branch.deepCopy();

            // get branch Meta information
            branchCopy.remove("contracts");
            branchCopy.remove("consensus");

            // save Meta information
            saveBranchMeta(branchId, branchCopy);

            if (save) {
                this.txReceipt.setStatus(ExecuteStatus.SUCCESS);
                this.txReceipt.addLog(String.format("Branch %s is created", branchId));
            } else {
                this.txReceipt.setStatus(ExecuteStatus.FALSE);
            }


            // check fee
            // check fee govonence


            // get validator

            // get meta information


        }

        /**
         * Returns the id of a updated branch
         *
         * @param params branchId The Id of the branch to update
         *               branch   The branch.json to update on the stem
         */
        @InvokeTransaction
        public void update(JsonObject params) {
            // TODO update branch meta information

            // get branch id
            String branchId = params.get("branchId").getAsString();

            // check branchId Exist


            // check branch validator
            // TODO getValidator
            ValidatorSet validatorSet = getValidator(branchId);

            JsonObject updateBranch = params.get("branch").getAsJsonObject();

            JsonObject originBranchMeta = getBranchMeta(branchId);

            if (originBranchMeta == null) {
                txReceipt.setStatus(ExecuteStatus.FALSE);
                txReceipt.addLog("branch is not exist");
            } else {
                originBranchMeta = metaMerge(originBranchMeta, updateBranch);
                // save branch meta information
                saveBranchMeta(branchId, originBranchMeta);
                txReceipt.setStatus(ExecuteStatus.SUCCESS);
            }
        }

        @InvokeTransaction
        public void updateValidator(JsonObject params) {
            // TODO update branch meta information

            // get branch id
            String branchId = params.get("branchId").getAsString();

            // check branch validator vote[]

            // save branch validator set information

        }

        public ValidatorSet getValidator(String branchId) {
            // TODO get validator Set
            // get Validator set

            //
            //this.state.get()
            return null;
        }



        private boolean saveBranch(String branchId, JsonObject branch) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);

            if (!this.state.contains(branchIdKey)) {
                this.state.put(branchIdKey, branch);
                return true;
            }
            return false;
        }

        private JsonObject getBranch(String branchId) {
            String branchIdKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
            return this.state.get(branchIdKey);
        }

        private void saveBranchMeta(String branchId, JsonObject branchMeta) {
            String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
            this.state.put(branchMetaKey, branchMeta);
        }

        private JsonObject getBranchMeta(String branchId) {
            String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
            return this.state.get(branchMetaKey);
        }

        public JsonObject metaMerge(JsonObject branchMeta, JsonObject branchMetaUpdate) {
            // update 가능 항목
            List<String> keyList = Arrays.asList(new String[]{"description"});
            keyList.stream().forEach(key -> {
                if (branchMeta.has(key) && branchMetaUpdate.has(key)) {
                    branchMeta.addProperty(key, branchMetaUpdate.get(key).getAsString());
                }
            });

            return branchMeta;
        }

        /**
         * Returns boolean
         *
         * @param branchId
         * */
        public void messageCall(String branchId) {
            // TODO message call to contract
            // TODO isEnoughFee
        }

        /**
         * @param params branch id
         *
         * @return branch json object
         */
        @ContractQuery
        public JsonObject getBranch(JsonObject params) {
            // TODO get branch information
            String branchId = params.get(BRANCH_ID).getAsString();
            JsonObject branch = getBranch(branchId);

            // TODO fee not enough mesaage
            return branch;
        }

        @ContractQuery
        public JsonObject getBranchMeta(JsonObject param) {
            String branchId = param.get(BRANCH_ID).getAsString();

            return getBranchMeta(branchId);
        }


        /**
         * @param params branch id
         *
         * @return contract json object
         */
        @ContractQuery
        public Set<JsonElement> getContract(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
            Set<JsonElement> contractSet = new HashSet<>();

            JsonObject branch = getBranch(branchId);
            if (branch != null) {
                JsonArray contracts = branch.get("contracts").getAsJsonArray();
                for (JsonElement c : contracts) {
                    contractSet.add(c);
                }
            }
            return contractSet;
        }

        /**
         * @param params branch id
         *
         * @return fee state
         */
        public BigInteger feeState(JsonObject params) {
            String branchId = params.get(BRANCH_ID).getAsString();
//            StemContractStateValue stateValue = getBranchStateValue(branchId);
//            BigDecimal result = BigDecimal.ZERO;
//            if (isBranchExist(branchId)) {
//                Long currentHeight = txReceipt.getBlockHeight();
//                Long createPointHeight = stateValue.getBlockHeight();
//                Long height = currentHeight - createPointHeight;
//
//                //1block to 1yeed
//                BigDecimal currentFee = stateValue.getFee();
//                result = currentFee.subtract(BigDecimal.valueOf(height));
//            }
//            return result.longValue() > 0 ? result : BigDecimal.ZERO;
            return BigInteger.ZERO;

        }

//        private BigInteger feeState(StemContractStateValue stateValue) {
//            BigInteger currentFee = stateValue.getFee();
//            if (currentFee.longValue() > 0) {
//                Long currentHeight = txReceipt.getBlockHeight();
//                Long createPointHeight = stateValue.getBlockHeight();
//                Long overTimeHeight = currentHeight - createPointHeight;
//                return currentFee.subtract(BigDecimal.valueOf(overTimeHeight));
//            }
//            return BigInteger.ZERO;
//        }

//        private Boolean isEnoughFee(StemContractStateValue stateValue) {
//            return feeState(stateValue).longValue() > 0;
//        }

        private boolean isBranchExist(String branchId) {
            return state.contains(branchId);
        }

        private boolean branchVerify(JsonObject branch) {
            // check property
            boolean verify = true;
            List<String> existProperty = Arrays.asList(new String[]{"name", "symbol", "property", "contracts"});

            for (String perpertyKey :existProperty) {
                verify &= branch.get(perpertyKey).isJsonNull() != true;
            }

            return verify;
        }
//
//        private StemContractStateValue getBranchStateValue(String branchId) {
//            JsonObject json = state.get(branchId);
//            if (json == null) {
//                return null;
//            } else {
//                return new StemContractStateValue(json);
//            }
//        }
    }
}
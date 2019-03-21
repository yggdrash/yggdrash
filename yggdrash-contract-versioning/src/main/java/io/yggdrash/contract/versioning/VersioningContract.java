package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.vo.dpoa.tx.TxValidatorVote;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.dpoa.DPoAContract;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

public class VersioningContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(VersioningContract.class);
    private BundleContext bundleContext = null;
    private ServiceTracker serviceTracker = null;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start contract version control");

        bundleContext = context;
        serviceTracker = new ServiceTracker(
                bundleContext
                , bundleContext.createFilter(
                "(&(objectClass=" + DPoAContract.DPoAService.class.getName() + ")" +
                        "(YGGDRASH=DPoA))"),
                null
        );
        serviceTracker.open();

        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "ContractVersionControl");
//        context.registerService(VersioningContractService.class.getName(), new VersioningContractService(serviceTracker), props);
        context.registerService(VersioningContractService.class.getName(), new VersioningContractService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop contract version control");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class VersioningContractService {
        private static final Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes
        private static final String SUFFIX_UPDATE_CONTRACT = "/update-temp-contracts";
        private static final String SUFFIX = ".jar";
//        private final ServiceTracker serviceTracker;

//        public VersioningContractService(ServiceTracker serviceTracker) {
//            this.serviceTracker = serviceTracker;
//        }

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @InvokeTransaction
        public TransactionReceipt updateProposer(JsonObject params) throws UnsupportedEncodingException {
            // TODO contract name 컨트랙트 조회 - branch store에서 조회
            String targetVersion = params.get("contractVersion").getAsString();

            // TODO target block height 저장


            String upgradeContract = params.get("contract").getAsString();
            byte[] binaryFile = base64Dec(upgradeContract.getBytes("UTF-8"));

            if ((binaryFile == null) || sizeVerify(binaryFile)) {
                return txReceipt;
            }
            FileOutputStream fos;

//            String containerPath = String.format("%s/%s", Path, branchId);
            String exportPath = System.getProperty("user.dir");
            String tempContractPath = String.format("%s/bundles%s", exportPath, SUFFIX_UPDATE_CONTRACT);

            File fileDir = new File(tempContractPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            ContractVersion version = ContractVersion.of(binaryFile);
            File destFile = new File(tempContractPath + File.separator + version + SUFFIX);
            destFile.setReadOnly();

            try {
                fos = new FileOutputStream(destFile);
                fos.write(binaryFile);
                fos.close();
            } catch (IOException e) {
                log.error(e.toString());
            }

            txReceipt.setStatus(ExecuteStatus.SUCCESS);

//            if (validatorVerify()) {
//            }
            return txReceipt;
        }

        @InvokeTransaction
        public TransactionReceipt vote(JsonObject params) {
            // TODO 배포 전 벨리데이터들 투표
            // TODO 투표하는 동안 validators들이 바뀔 수 있다.
            // TODO txId로 투표
            // TODO 임시 폴더 파일 위치
            // TODO 2/3이상 투표 완료시 임시폴더 파일 컨트랙트 폴더 위치로 이동
            // TODO 2/3이상일 경우 투표 마감
            // 과반수가 투표 반대일경우

            // txid 별 issuer 투표상태 저장
            // 투표 찬성 수 체크
            // 과반수 이상 동의시 브랜치 스토어에 저장
            txReceipt.setStatus(ExecuteStatus.FALSE);
            //Check validation
            TxValidatorVote txValidatorVote = JsonUtil.generateJsonToClass(params.toString(), TxValidatorVote.class);
//            if (!validateTx(txValidatorVote)) {
//                return txReceipt;
//            }
//
//            //Is exists proposed validator
//            ProposeValidatorSet proposeValidatorSet = getProposeValidatorSet();
//            if (proposeValidatorSet == null || MapUtils.isEmpty(proposeValidatorSet.getValidatorMap()) || proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr()) == null) {
//                return txReceipt;
//            }
//
//            //Check available vote
//            ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap().get(txValidatorVote.getValidatorAddr());
//            if (votable.getVotedMap().get(txReceipt.getIssuer()) == null || votable.getVotedMap().get(txReceipt.getIssuer()).isVoted()) {
//                return txReceipt;
//            }
//
//            //Vote
//            if (txValidatorVote.isAgree()) {
//                votable.setAgreeCnt(votable.getAgreeCnt() + 1);
//            } else {
//                votable.setDisagreeCnt(votable.getDisagreeCnt() + 1);
//            }
//            votable.getVotedMap().get(txReceipt.getIssuer()).setAgree(txValidatorVote.isAgree());
//            votable.getVotedMap().get(txReceipt.getIssuer()).setVoted(true);
//
//            //Save
//            state.put(PrefixKeyEnum.PROPOSE_VALIDATORS.toValue(), JsonUtil.parseJsonObject(JsonUtil.convertObjToString(proposeValidatorSet)));
//            txReceipt.setStatus(ExecuteStatus.SUCCESS);


            return txReceipt;
        }

        @ContractQuery
        public void updateStatus() {
            //TODO 투표 상태 및 업데이트 된지 안된지 상태
        }


//        private boolean validatorVerify() {
//            DPoAContract.DPoAService dPoAService = (DPoAContract.DPoAService) serviceTracker.getService();
////            serviceTracker.waitForService(5000);
//            dPoAService.getValidatorSet();
//
//            ValidatorSet validatorSet = dPoAService.getValidatorSet();
//            if (validatorSet == null || validatorSet.getValidatorMap() == null
//                    || validatorSet.getValidatorMap().get(txReceipt.getIssuer()) == null) {
//                return false;
//            }
//            return true;
//        }

        private boolean sizeVerify(byte[] binaryFile) {
            if (binaryFile.length > MAX_FILE_LENGTH) {
                return false;
            }
            return true;
        }

        private static byte[] base64Dec(byte[] buffer) {
            return Base64.decodeBase64(buffer);
        }
    }
}

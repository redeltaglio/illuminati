package me.phoboslabs.illuminati.common;

import me.phoboslabs.illuminati.common.constant.IlluminatiConstant;
import me.phoboslabs.illuminati.common.dto.enums.IlluminatiStorageType;
import me.phoboslabs.illuminati.common.properties.IlluminatiCommonProperties;
import me.phoboslabs.illuminati.common.properties.IlluminatiPropertiesHelper;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;
import me.phoboslabs.illuminati.common.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IlluminatiCommon {

    private final static Logger COMMON_LOGGER = LoggerFactory.getLogger(IlluminatiCommon.class);

    private static final String H2_CLASS_NAME = "org.h2.Driver";

    private static final List<IlluminatiStorageType> ILLUMINATI_BACKUP_CONFIGURATION_CLASS_NAME_ARRAY;
    static {
        List<IlluminatiStorageType> classNameArray;
        try {
            classNameArray = Arrays.asList(IlluminatiStorageType.getEnumType(H2_CLASS_NAME));
        } catch (Exception ex) {
            classNameArray = new ArrayList<>();
            COMMON_LOGGER.error("check backup stage type.");
        }
        ILLUMINATI_BACKUP_CONFIGURATION_CLASS_NAME_ARRAY = Collections.unmodifiableList(classNameArray);
    }

    public synchronized static void init () {
        final String debug = IlluminatiPropertiesHelper.getPropertiesValueByKey(IlluminatiCommonProperties.class, "illuminati", "debug", null);
        if (StringObjectUtils.isValid(debug)) {
            IlluminatiConstant.ILLUMINATI_DEBUG = Boolean.valueOf(debug);
        }

        if (IlluminatiConstant.ILLUMINATI_DEBUG) {
            SystemUtil.createThreadStatusDebugThread();
        }

        if (IlluminatiPropertiesHelper.isIlluminatiSwitcherActive()) {
            IlluminatiConstant.ILLUMINATI_SWITCH_ACTIVATION = true;
        }

        for (IlluminatiStorageType illuminatiStorageType : ILLUMINATI_BACKUP_CONFIGURATION_CLASS_NAME_ARRAY) {
            try {
                Class.forName(illuminatiStorageType.getType());
                IlluminatiConstant.ILLUMINATI_BACKUP_STORAGE_TYPE = illuminatiStorageType;
                IlluminatiConstant.ILLUMINATI_BACKUP_ACTIVATION = true;
                break;
            } catch (ClassNotFoundException e) {
                //my class isn't there!
                IlluminatiConstant.ILLUMINATI_BACKUP_ACTIVATION = false;
            }
        }
    }
}

package cn.yyxx.support;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cn.yyxx.support.hawkeye.LogUtils;

/**
 * @author #Suyghur.
 * Created on 2021/04/23
 */
public class PropertiesUtils {


    private static Map<String, Properties> propertiesMapCache = null;

    public static Properties getProperties(Context context, String fileName, Location location) {
        Properties proFile = null;
        InputStream in = null;
        try {
            proFile = new Properties();
            switch (location) {
                case ASSETS:
                    in = FileUtils.accessFileFromAssets(context, fileName);
                    break;
                case META_INF:
                    in = FileUtils.accessFileFromMetaInf(context, fileName);
                    break;
                default:
                    LogUtils.e("get properties failed , file location is error");
                    break;
            }
            if (in != null) {
                proFile.load(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return proFile;
    }


    public static String accessProFromAssets(Context context, String fileName, String key) {
        Properties properties = getProperties(context, fileName, Location.ASSETS);
        if (properties != null) {
            propertiesMapCache.put(fileName, properties);
            return properties.getProperty(key);
        } else {
            return "";
        }
    }

    public static String accessProFromMetaInf(Context context, String fileName, String key) {
        Properties properties = getProperties(context, fileName, Location.META_INF);
        if (properties != null) {
            propertiesMapCache.put(fileName, properties);
            return properties.getProperty(key);
        } else {
            return "";
        }
    }

    public static String getValue4Properties(Context context, String fileName, String key) {
        return getValue4Properties(context, fileName, "", key);
    }


    public static String getValue4Properties(Context context, String fileName, String path, String key) {
        if (propertiesMapCache == null) {
            propertiesMapCache = new HashMap<>();
        }
        String value = null;

//        //??????ID????????????META-INF/???????????????id
//        //???????????????????????????package_??????id???????????????
//        //SDK????????????META-INF??????????????????package_????????????????????????????????????????????????????????????????????????id
//        //??????META-INF??????????????????????????????????????????????????????
//        if (key.equals("3KWAN_PackageID")) {
//            if (mMetaInfPackageId > 0) {
//                return mMetaInfPackageId + "";
//            } else if (mMetaInfPackageId == -1) {
//                LogUtils.d("???????????????????????????Assets??????...");
//            } else {
//                mMetaInfPackageId = getPackageIdFromMetainf(context);
//                LogUtils.d("???META-INF?????????packageId = " + mMetaInfPackageId);
//                if (mMetaInfPackageId == 0) {
//                    mMetaInfPackageId = -1;
//                }
//                if (mMetaInfPackageId > 0) {
//                    return mMetaInfPackageId + "";
//                }
//                LogUtils.d("???????????????????????????Assets??????...");
//            }
//        }

        if (propertiesMapCache.containsKey(fileName)) {
            try {
                value = propertiesMapCache.get(fileName).getProperty(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
            LogUtils.d("?????????????????????" + key + ":" + value);
            return value;
        }


        if (FileUtils.isExistInAssets(context, fileName, path)) {
            if (TextUtils.isEmpty(path)) {
                value = accessProFromAssets(context, fileName, key);
            } else {
                value = accessProFromAssets(context, path + "/" + fileName, key);
            }
        }
//        if (FileUtils.isExistInMetaInf(context, fileName)) {
//            value = accessProFromMetaInf(context, fileName, key);
//        }
        return value;
    }

    /**
     * ???META-INF/???????????????id???package_xxx???xxx?????????ID
     */
    public static int getPackageIdFromMetainf(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String sourceDir = appInfo.sourceDir;
        //LogUtils.d("sourceDir = " + sourceDir);
        ZipFile zipfile = null;
        int packageId = 0;
        try {
            zipfile = new ZipFile(sourceDir);
            Enumeration<?> entries = zipfile.entries();
            a:
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.contains("../")) {
                    break;
                }
                //LogUtils.d("entryName = " + entryName);
                //???META-INF/package_??????
                if (entryName.contains("META-INF/package_")) {
                    // ???????????????????????????
                    // ??????ZipInputStream????????????
                    String packageIdStr = entryName.split("_")[1];
                    //LogUtils.d("packageIdStr = " + packageIdStr);
                    packageId = Integer.parseInt(packageIdStr);
                    break a;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return packageId;
    }

    public enum Location {
        ASSETS,
        META_INF
    }
}

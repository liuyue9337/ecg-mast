package com.ljfth.ecgviewlib;

public class Constant {
    // 姓名
    public static final String SP_NAME = "sp_name";
    // 年龄
    public static final String SP_AGE = "sp_age";
    // 性别
    public static final String SP_SEX = "sp_sex";
    // 住院号
    public static final String SP_H_NUM = "sp_h_num";
    // 床号
    public static final String SP_BED_NUM = "sp_bed_num";
    // 起搏
    public static final String SP_PACE_MAKING = "sp_pace_making";
    // 血氧
    public static final String SP_SPO2_UPPER = "sp_spo2_upper";
    public static final String SP_SPO2_FLOOR = "sp_spo2_floor";
    // 心率
    public static final String SP_ECG_UPPER = "sp_ecg_upper";
    public static final String SP_ECG_FLOOR = "sp_ecg_floor";
    // 呼吸
    public static final String SP_RESP_UPPER = "sp_resp_upper";
    public static final String SP_RESP_FLOOR = "sp_resp_floor";
    // 温度
    public static final String SP_TEMP_UPPER = "sp_temp_upper";
    public static final String SP_TEMP_FLOOR = "sp_temp_floor";

    // 收缩压
    public static final String SP_SBP_UPPER = "sp_sbp_upper";
    public static final String SP_SBP_FLOOR = "sp_sbp_floor";
    // 舒张压
    public static final String SP_DBP_UPPER = "sp_Dbp_upper";
    public static final String SP_DBP_FLOOR = "sp_Dbp_floor";
    // 平均压
    public static final String SP_MAP_UPPER = "sp_map_upper";
    public static final String SP_MAP_FLOOR = "sp_map_floor";
    // 报警使能
    public static final String SP_RING = "sp_ring";

    // 服务器IP
    public static final String SP_IP = "Server_Ip";

    // 服务器Port
    public static final String SP_PORT = "Server_Port";

    public static final String ServerIp = "39.98.220.57";
    public static final String ServerPort = "9099";




    public static final int SPO2TOP = 100;
    public static final int SPO2BOTTOM = 80;

    public static final int ECGTOP = 130;
    public static final int ECGBOTTOM = 50;

    public static final int RESPTOP = 30;
    public static final int RESPBOTTOM = 5;

    public static final int TEMPTOP = 38;
    public static final int TEMPBOTTOM = 35;

    public static final int SBPTOP = 150;
    public static final int SBPBOTTOM = 50;

    public static final int DBPTOP = 150;
    public static final int DBPBOTTOM = 50;

    public static final int MAPTOP = 40;
    public static final int MAPBOTTOM = 30;


    /**
     * 性别常量（男）
     */
    public static final int SEX_MEN = 0x001;
    /**
     * 性别常量（女）
     */
    public static final int SEX_WOMEN = 0x002;
    /**
     * 起搏常量（是）
     */
    public static final int PACE_YES = 0x003;
    /**
     * 起搏常量（否）
     */
    public static final int PACE_NO = 0x004;

    /**搜索*/
    public static final int TYPE_SEARCH = 0;
    /**连接*/
    public static final int TYPE_CONNECT = 1;
    /**检测*/
    public static final int TYPE_DETECTION = 2;
    /**断开*/
    public static final int TYPE_BREAK = 3;
    /**重置*/
    public static final int TYPE_RESTORATION = 4;
    /**断开所有连接*/
    public static final int TYPW_DISC_ALL_DEV = 15;
    /**获得心跳数据*/
    public static final int TYPE_NET_HEART = 16;
    /**NIBP开始测量*/
    public static final int TYPE_NIBP_Start = 8;
    /**NIBP停止测量*/
    public static final int TYPE_NIBP_Stop = 9;
    /**HPMS-NIBP-Start,开始HPMS无创血压测量*/
    public static final int TYPE_HPMS_NIBP_Start = 13;
    /**HPMS-NIBP-Start,停止HPMS无创血压测量*/
    public static final int TYPE_HPMS_NIBP_Stop = 14;


    public static final String DEVICES_TYPE_BODYSTMSPO2 = "0";    //血氧
    public static final String DEVICES_TYPE_BODYSTMECG = "1";     //心电
    public static final String DEVICES_TYPE_BODYSTMNIBP_PWV = "2";    //连续血压
    public static final String DEVICES_TYPE_BODYSTMRESP = "3";        //呼吸
    public static final String DEVICES_TYPE_BODYSTMTEMP1 = "4";       //温度
    public static final String DEVICES_TYPE_BODYSTMTEMP2 = "5";       //温度
    public static final String DEVICES_TYPE_BODYSTMNIBP_VAL = "6";    //无创血压

    public static String SAVE_PATH;

    public static int TagetSampled4Draw = 80;

    //为条件编译用
    public static final boolean SPO2_FUNC_ENABLE = true;
    public static final boolean ECG_FUNC_ENABLE = true;
    public static final boolean RESP_FUNC_ENABLE = true;
    public static final boolean NIBP_FUNC_ENABLE = true;

    //传感器设备类型
    public static final int BLE_DEV_TEMP = 0;
    public static final int BLE_DEV_SPO2 = 1;
    public static final int BLE_DEV_ECG = 2;
    public static final int BLE_DEV_NIBP = 3;
    public static final int BLE_DEV_HPMS = 4;
    public static final int BLE_DEV_MONITOR = 5;
    public static final int BLE_DEV_MAX = 6;

    //生命体征参数类型
    public static final int VitalSignTEMP = 0;	//温度
    public static final int VitalSignSPO2 = 1;	//血氧
    public static final int VitalSignECG = 2;	//心电
    public static final int VitalSignResp = 3;	//呼吸
    public static final int VitalSignNIBP = 4;	//无创血压
    public static final int VitalSignIBP = 5;	//有创血压
    public static final int VitalSignEtCO2 = 6;	//呼末二氧化碳

    //波形类型
    public static final int WaveTypeRed = 0;
    public static final int WaveTypeIred = 1;
    public static final int WaveTypeEcgI = 2;
    public static final int WaveTypeEcgII = 3;
    public static final int WaveTypeEcgIII = 4;
    public static final int WaveTypeEcgV = 5;
    public static final int WaveTypeEcgavR = 6;
    public static final int WaveTypeEcgavF = 7;
    public static final int WaveTypeEcgavL = 8;
    public static final int WaveTypeResp = 9;
    public static final int WaveTypeEtCO2 = 10;
    public static final int WaveTypeIBP = 11;

    //生命体征参数信息
    public static final int VitalSignParamSpO2 = 0;		//血氧值
    public static final int VitalSignParamPR = 1;		//脉率值
    public static final int VitalSignParamPI = 2;		//灌度指数
    public static final int VitalSignParamHR = 3;		//心率值
    public static final int VitalSignParamRR = 4;		//呼吸率
    public static final int VitalSignParamTemp = 5;		//体温
    public static final int VitalSignParamSBP = 6;		//收缩压NIBP
    public static final int VitalSignParamDBP = 7;		//舒张压NIBP
    public static final int VitalSignParamABP = 8;		//平均压NIBP
    public static final int VitalSignParamSBP4IBP = 9;		//收缩压IBP
    public static final int VitalSignParamDBP4IBP = 10;		//舒张压IBP
    public static final int VitalSignParamABP4IBP = 11;		//平均压IBP
    public static final int VitalSignParamEtCO2 = 12;		//EtCO2
    public static final int VitalSignParamFiCO2 = 13;		//FiCO2
    public static final int VitalSignParamawRR = 14;		//awRR

}

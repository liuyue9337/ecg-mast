#include "com_algorithm4_library_algorithm4library_Algorithm4SensorLib.h"

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    InitSingleInstance
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_InitSingleInstance
  (JNIEnv *, jclass) {
    jboolean b = false;
    b = InitSingleInstance();
    return b;
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    SetTargetSampling
 * Signature: ([I)V
 */
JNIEXPORT void JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_SetTargetSampling
  (JNIEnv *env, jclass thiz, jintArray intArray){
    int nLen = 0;
    nLen = env->GetArrayLength(intArray);
	if (nLen <= 0) {
		nLen = 7;
	}
	//nLen = (nLen > 4 ? : nLen);
	//int data[7];
	int *data = (int*)malloc(nLen * sizeof(int));

    env->GetIntArrayRegion(intArray,0,nLen,data);
	SetTargetSampling(data, 0);
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    getValue
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_getValue
  (JNIEnv *env, jclass thiz, jdoubleArray doubleArray){
    int nLen = env->GetArrayLength(doubleArray);
    double *Data = new double[nLen];
    env->GetDoubleArrayRegion(doubleArray,0,nLen,Data);
    getValue(Data, 0);

    jint count = env->GetArrayLength(doubleArray);
    env->SetDoubleArrayRegion(doubleArray, 0, count, Data);

    //    env->NewDoubleArray(count);
    	//int i = 0;
    	//__android_log_print(ANDROID_LOG_DEBUG, "ljfth", "Count is=%d\n", count);

      //for (i = 0; i < (int)Data[0]; i++)
    // {
    		// __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "value i=%d  value is =%lf", i, Data[i]);

             //得到一个jstring的数据
             //jstring str = (*env)->NewStringUTF(env, temp);

             //将str设置给arrayStringData的第i个元素。
             //(*env)->SetObjectArrayElement(env, arrayStringData, i, str);
    // }
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    getSampledData
 * Signature: ([[D[D)V
 */
 JNIEXPORT void JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_getSampledData
   (JNIEnv *env, jclass thiz, jdoubleArray doubleArray){
	int nLen = env->GetArrayLength(doubleArray);
    double *Data = new double[nLen];
    env->GetDoubleArrayRegion(doubleArray,0,nLen,Data);
    getSampledData((double (*)[1024])Data, 0);
    env->SetDoubleArrayRegion(doubleArray, 0, nLen, Data);
    //    __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "arraySize=%d  nLen=%d", arraySize, nLen);
    //    __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "nProtocolLen=%d  nArrayLen=%d", nProtocolLen, nArrayLen);
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    addRecvData
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_addRecvData
  (JNIEnv *env, jclass thiz, jbyteArray byteArray, jint nArrayLen){
      int nLen = 0;
      nLen = env->GetArrayLength(byteArray);
      nLen = nLen > nArrayLen ? nArrayLen : nLen;
      unsigned char *data = new unsigned char [nLen];

      env->GetByteArrayRegion(byteArray,0,nLen,(signed char *)data);
      //        for(int j  = 0; j < nArrayLen; j++)
      //        {
      //            __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "data=%d", data[i][j]);
      //        }

      //    __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "arraySize=%d  nLen=%d", arraySize, nLen);
      //    __android_log_print(ANDROID_LOG_DEBUG, "ljfth", "nProtocolLen=%d  nArrayLen=%d", nProtocolLen, nArrayLen);
      int nRet = 0;
      nRet = addRecvData((unsigned char *)data, nLen, 0);
      delete []data;
      return nRet;
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    GeneralCmd4Dev
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_GeneralCmd4Dev
  (JNIEnv *env, jclass thiz, jint nCommandType, jbyteArray byteArray){
    //Get Command MSG
	int nLen = 0;
    nLen = env->GetArrayLength(byteArray);
	unsigned char *data = new unsigned char [SupLength];
    env->GetByteArrayRegion(byteArray,0,nLen,(signed char *)data);

	jboolean b = false;
	b = GeneralCmd4Dev((CommandType_e)nCommandType, (char *)data, 0);

	//Copy Result 2 Java Space by param(byteArray)
    env->SetByteArrayRegion(byteArray, 0, SupLength, (signed char *)data);

	return data[0];
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    GetCmdResult
 * Signature: (I[B)Z
 */
JNIEXPORT jboolean JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_GetCmdResult
  (JNIEnv *env, jclass thiz, jint nCommandType, jbyteArray byteArray){
    //Get Command MSG
	int nLen = 0;
    nLen = env->GetArrayLength(byteArray);
	unsigned char *data = new unsigned char [SupLength];
    env->GetByteArrayRegion(byteArray,0,nLen,(signed char *)data);

	jboolean b = false;
	b = GetCmdResult((CommandType_e)nCommandType, (char *)data, 0);

	__android_log_print(ANDROID_LOG_DEBUG, "ljfth", "Return=%d  datalen=%d", (b == true ? 1 : 0), data[0]);

	//Copy Result 2 Java Space by param(byteArray)
    env->SetByteArrayRegion(byteArray, 0, SupLength, (signed char *)data);
	return b;
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    GetSensorMsg
 * Signature: ([B)Z
 */
JNIEXPORT jboolean JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_GetSensorMsg
  (JNIEnv *env, jclass thiz, jbyteArray byteArray){
    int nLen = 256;
    nLen = env->GetArrayLength(byteArray);
	if (nLen < 256)
		return 0;
	else if (nLen > 256)
		nLen = 256;

	//将参数映射到内部空间
	unsigned char *data = new unsigned char [nLen];
    env->GetByteArrayRegion(byteArray,0,nLen,(signed char *)data);

	//获取传感器信息
	getSensorMsg(data, 1, 0);

	//返回传感器信息
	env->SetByteArrayRegion(byteArray, 0, nLen, (signed char *)data);
	return 0;
  }

/*
 * Class:     com_algorithm4_library_algorithm4library_Algorithm4SensorLib
 * Method:    GetCentorMac
 * Signature: ([B)I
 */

JNIEXPORT jint JNICALL Java_com_algorithm4_library_algorithm4library_Algorithm4SensorLib_GetCentorMac
  (JNIEnv *env, jclass thiz, jbyteArray byteArray){
    //Get Centor Mac Addr
	int nLen = 0;
    nLen = env->GetArrayLength(byteArray);
	if (nLen > 6)
		nLen = 6;
	else if (nLen < 6)
		return 0;

	//将参数映射到内部空间
	unsigned char *data = new unsigned char [6];
    env->GetByteArrayRegion(byteArray,0,nLen,(signed char *)data);

	//获取Mac地址
	GetDeviceMac(data, 0);

	//返回Mac地址
	env->SetByteArrayRegion(byteArray, 0, 6, (signed char *)data);
	return 0;
  }


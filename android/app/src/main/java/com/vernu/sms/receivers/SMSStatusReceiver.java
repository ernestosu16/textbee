package com.vernu.sms.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.vernu.sms.AppConstants;
import com.vernu.sms.dtos.SMSDTO;
import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.workers.SMSStatusUpdateWorker;
import com.vernu.sms.R;


public class SMSStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSStatusReceiver";
    
    public static final String SMS_SENT = "SMS_SENT";
    public static final String SMS_DELIVERED = "SMS_DELIVERED";
    
    /**
     * Resolves a result code to the constant name (e.g. SmsManager.RESULT_ERROR_GENERIC_FAILURE)
     * via reflection. Returns null if no matching constant is found.
     */
    private static String getResultCodeName(int resultCode) {
        for (Class<?> clazz : new Class<?>[]{ SmsManager.class, Activity.class }) {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getType() != int.class) continue;
                    if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) continue;
                    if (!field.getName().startsWith("RESULT_")) continue;
                    field.setAccessible(true);
                    if (field.getInt(null) == resultCode) {
                        return clazz.getSimpleName() + "." + field.getName();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Reflection failed for " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
        return null;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String smsId = intent.getStringExtra("sms_id");
        String smsBatchId = intent.getStringExtra("sms_batch_id");
        String action = intent.getAction();
        
        SMSDTO smsDTO = new SMSDTO();
        smsDTO.setSmsId(smsId);
        smsDTO.setSmsBatchId(smsBatchId);
        
        if (SMS_SENT.equals(action)) {
            handleSentStatus(context, intent, getResultCode(), smsDTO);
        } else if (SMS_DELIVERED.equals(action)) {
            handleDeliveredStatus(context, getResultCode(), smsDTO);
        }
    }
    
    private void handleSentStatus(Context context, Intent intent, int resultCode, SMSDTO smsDTO) {
        long timestamp = System.currentTimeMillis();
        String errorMessage;
        
        switch (resultCode) {
            case Activity.RESULT_OK:
                smsDTO.setStatus("SENT");
                smsDTO.setSentAtInMillis(timestamp);
                Log.d(TAG, "SMS sent successfully - ID: " + smsDTO.getSmsId());
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                errorMessage = context.getString(R.string.sms_error_generic_failure);
                int radioCode = intent.getIntExtra("errorCode", -1);
                if (radioCode != -1) {
                    errorMessage += " (code " + radioCode + ")";
                }
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                errorMessage = context.getString(R.string.sms_error_radio_off);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                errorMessage = context.getString(R.string.sms_error_null_pdu);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                errorMessage = context.getString(R.string.sms_error_no_service);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                errorMessage = context.getString(R.string.sms_error_limit_exceeded);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                errorMessage = context.getString(R.string.sms_error_short_code_not_allowed);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                errorMessage = context.getString(R.string.sms_error_short_code_never_allowed);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            case SmsManager.RESULT_NETWORK_ERROR:
                errorMessage = context.getString(R.string.sms_error_network_error);
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            default:
                String codeName = getResultCodeName(resultCode);
                errorMessage = codeName != null ? codeName : (context.getString(R.string.sms_error_unknown, resultCode));
                smsDTO.setStatus("FAILED");
                smsDTO.setFailedAtInMillis(timestamp);
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS failed to send - ID: " + smsDTO.getSmsId() + ", Error: " + errorMessage);
                break;
        }
        
        updateSMSStatus(context, smsDTO);
    }
    
    private void handleDeliveredStatus(Context context, int resultCode, SMSDTO smsDTO) {
        long timestamp = System.currentTimeMillis();
        String errorMessage;
        
        switch (resultCode) {
            case Activity.RESULT_OK:
                smsDTO.setStatus("DELIVERED");
                smsDTO.setDeliveredAtInMillis(timestamp);
                Log.d(TAG, "SMS delivered successfully - ID: " + smsDTO.getSmsId());
                break;
            case Activity.RESULT_CANCELED:
                errorMessage = context.getString(R.string.sms_delivery_error_canceled);
                smsDTO.setStatus("DELIVERY_FAILED");
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS delivery failed - ID: " + smsDTO.getSmsId() + ", Error code: " + resultCode + ", Error: " + errorMessage);
                break;
            default:
                String deliveryCodeName = getResultCodeName(resultCode);
                errorMessage = deliveryCodeName != null ? deliveryCodeName : (context.getString(R.string.sms_delivery_error_unknown, resultCode));
                smsDTO.setStatus("DELIVERY_FAILED");
                smsDTO.setErrorCode(String.valueOf(resultCode));
                smsDTO.setErrorMessage(errorMessage);
                Log.e(TAG, "SMS delivery failed - ID: " + smsDTO.getSmsId() + ", Error: " + errorMessage);
                break;
        }
        
        updateSMSStatus(context, smsDTO);
    }
    
    private void updateSMSStatus(Context context, SMSDTO smsDTO) {
        String deviceId = SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");
        String apiKey = SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_API_KEY_KEY, "");
        
        if (deviceId.isEmpty() || apiKey.isEmpty()) {
            Log.e(TAG, "Device ID or API key not found");
            return;
        }

        SMSStatusUpdateWorker.enqueueWork(context, deviceId, apiKey, smsDTO);
    }
} 
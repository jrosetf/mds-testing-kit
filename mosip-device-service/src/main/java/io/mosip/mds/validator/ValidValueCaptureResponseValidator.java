package io.mosip.mds.validator;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jose4j.lang.JoseException;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.mds.dto.CaptureResponse;
import io.mosip.mds.dto.CaptureResponse.CaptureBiometricData;
import io.mosip.mds.dto.ValidateResponseRequestDto;
import io.mosip.mds.entitiy.Validator;

public class ValidValueCaptureResponseValidator extends Validator {

	private final List<String> bioSubTypeFingerList= getBioSubTypeFinger();
	private final List<String> bioSubTypeIrisList = getBioSubTypeIris();
	public ValidValueCaptureResponseValidator() {
		super("ValidValueCaptureResponseValidator", "Valid Value Capture Response Validator");
	}
	private static ObjectMapper mapper=new ObjectMapper();;
	
	@Override
	protected List<String> DoValidate(ValidateResponseRequestDto response) {
		List<String> errors = new ArrayList<>();
		if(Objects.isNull(response))
		{
			errors.add("Response is empty");
			return errors;
		}
		
		errors=validateCaptureSignatureTampered(response, errors);
		if(errors.size()!=0)return errors;

		CaptureResponse cr = (CaptureResponse) response.getMdsDecodedResponse();

		if(Objects.isNull(cr))
		{
			errors.add("Capture Response is empty");
			return errors;
		}
		
		
		
		if(cr.biometrics == null || cr.biometrics.length == 0)
		{
			errors.add("Capture response does not contain biometrics block");
			return errors;
		}

		//
		for(CaptureResponse.CaptureBiometric bb:cr.biometrics)
		{
			CaptureBiometricData dataDecoded = bb.dataDecoded;
			if(Objects.nonNull(dataDecoded)) {
				errors=validateActualValueDatadecoded(errors, dataDecoded);
				if(errors.size()!=0)return errors;

				//TODO check for env
				if( !dataDecoded.env.equals(CommonConstant.STAGING) && !dataDecoded.env.equals(CommonConstant.DEVELOPER)
						&& !dataDecoded.env.equals(CommonConstant.PRE_PRODUCTION) && !dataDecoded.env.equals(CommonConstant.PRODUCTION))
				{
					errors.add("Capture response biometrics-dataDecoded env is invalid");
					return errors;
				}

				//TODO check time stamp for ISO Format date time with timezone
				errors=CommonValidator.validateTimeStamp(dataDecoded.timestamp,errors);
				if(errors.size()!=0)return errors;

				//TODO check for requestedScore
				//TODO check for quality score
			}
		}
		return errors;
	}
	private List<String> validateCaptureSignatureTampered(ValidateResponseRequestDto response, List<String> errors) {
		CaptureResponse mdsResponse = null;
		try {
			mdsResponse = (CaptureResponse) (mapper.readValue(response.mdsResponse.getBytes(), CaptureResponse.class));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (CaptureResponse.CaptureBiometric biometric : mdsResponse.biometrics) {

			if (biometric.getData() != null) {
				try {
					if(CommonValidator.validateSignature(biometric.getData())) {
						errors.add("mdsResponse signature verification failed");
						return errors;
					}
				} catch (CertificateException | JoseException | IOException e) {
					errors.add("mdsResponse with Invalid Signature");
					return errors;
					//e.printStackTrace();
				}
			}
		}
		return errors;
	}
	private List<String> validateActualValueDatadecoded(List<String> errors, CaptureBiometricData dataDecoded) {
		// Check for bioType elements
		if(!dataDecoded.bioType.equals(CommonConstant.FINGER) && !dataDecoded.bioType.equals(CommonConstant.IRIS) && !dataDecoded.bioType.equals(CommonConstant.FACE))
		{
			errors.add("Capture response biometrics-dataDecoded bioType is invalid");
			return errors;
		}else {

			//Check for bioSubType
			errors = validateBioSubType(errors, dataDecoded);
			if(!ObjectUtils.isEmpty(errors))
				return errors;
		}
		//Check for purpose elements
		if(!dataDecoded.purpose.equals(CommonConstant.AUTH) && !dataDecoded.purpose.equals(CommonConstant.REGISTRATION) )
		{
			errors.add("Capture response biometrics-dataDecoded purpose is invalid");
			return errors;
		}

		//TODO Check for digitalId dataDecoded.digitalId

		return errors;
	}

	private List<String> validateBioSubType(List<String> errors, CaptureBiometricData dataDecoded) {
		// Check for bioSubType of Finger elements
		if(dataDecoded.bioType.equals(CommonConstant.FINGER) &&
				!bioSubTypeFingerList.contains(dataDecoded.bioSubType))
		{
			errors.add("Capture response biometrics bioSubType is invalid for Finger");
			return errors;
		}
		// Check for bioSubType of Iris elements
		if(dataDecoded.bioType.equals(CommonConstant.IRIS) &&
				!bioSubTypeIrisList.contains(dataDecoded.bioSubType))
		{
			errors.add("Capture response biometrics bioSubType is invalid for Iris");
			return errors;
		}
		// Check for bioSubType of Face elements
		if(dataDecoded.bioType.equals(CommonConstant.FACE) &&
				!(dataDecoded.bioSubType == null || dataDecoded.bioSubType.isEmpty()))
		{
			errors.add("Capture response biometrics bioSubType is invalid for Face");
			return errors;
		}

		errors=validateDigitalId(dataDecoded, errors);
		return errors;
	}

	private List<String> validateDigitalId(CaptureBiometricData dataDecoded,List<String> errors) {
		CommonValidator commonValidator=new CommonValidator();
		errors = commonValidator.validateSignedDigitalID(dataDecoded.digitalId);
		return errors;
	}

	public List<String> getBioSubTypeIris() {
		List<String> bioSubTypeIrisList = new ArrayList<String>();
		bioSubTypeIrisList.add(CommonConstant.LEFT);
		bioSubTypeIrisList.add(CommonConstant.RIGHT);
		bioSubTypeIrisList.add( CommonConstant.UNKNOWN);
		return bioSubTypeIrisList;
	}
	public List<String> getBioSubTypeFinger() {
		List<String> bioSubTypeFingerList=new ArrayList<String>();
		bioSubTypeFingerList.add(CommonConstant.LEFT_INDEX_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_MIDDLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_RING_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_LITTLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_THUMB);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_INDEX_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_MIDDLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_RING_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_LITTLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_THUMB);
		bioSubTypeFingerList.add(CommonConstant.UNKNOWN);

		return bioSubTypeFingerList;
	}
}

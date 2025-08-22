package app.model.dto.request;

import app.model.entity.enums.ReqType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalNotNullValidator implements ConstraintValidator<ConditionalNotNull, AiRequest> {

	@Override
	public boolean isValid(AiRequest request, ConstraintValidatorContext context) {
		if (request.getReqType() == ReqType.MENU_DESCRIPTION) {
			if (request.getMenuName() == null || request.getMenuName().isBlank()) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("MENU_DESCRIPTION 요청은 메뉴 이름이 필수입니다.")
					.addPropertyNode("menuName")
					.addConstraintViolation();
				return false;
			}
		}
		return true;
	}
}

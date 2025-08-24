import {
  registerDecorator,
  ValidationOptions,
  ValidatorConstraint,
  ValidatorConstraintInterface,
  ValidationArguments,
} from "class-validator";
import { Transform } from "class-transformer";
import { ObjectId } from "mongodb";

@ValidatorConstraint({ async: false })
export class IsObjectIdConstraint implements ValidatorConstraintInterface {
  validate(value: any, args: ValidationArguments): boolean {
    if (Array.isArray(value)) {
      return value.every((item) => ObjectId.isValid(item));
    }
    return ObjectId.isValid(value);
  }

  defaultMessage(args: ValidationArguments): string {
    return `Each value in ${args.property} must be a valid MongoDB ObjectId.`;
  }
}

export function IsObjectId(validationOptions?: ValidationOptions) {
  return function (object: Object, propertyName: string) {
    registerDecorator({
      target: object.constructor,
      propertyName,
      options: validationOptions,
      constraints: [],
      validator: IsObjectIdConstraint,
    });

    Transform(({ value }) => {
      if (Array.isArray(value)) {
        return value.map((item) => (ObjectId.isValid(item) ? new ObjectId(item) : item));
      }
      return ObjectId.isValid(value) ? new ObjectId(value) : value;
    })(object, propertyName);
  };
}

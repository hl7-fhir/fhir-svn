﻿using Hl7.Fhir.Model;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace Hl7.Fhir.Validation
{
    [AttributeUsage(AttributeTargets.Property, Inherited = false, AllowMultiple = false)]
    public class OidPatternAttribute : ValidationAttribute
    {
        protected override ValidationResult IsValid(object value, ValidationContext validationContext)
        {
            if (validationContext.ObjectType != typeof(string))
                throw new ArgumentException("OidPatternAttribute can only be applied to string properties");

            if (OidPatternAttribute.IsValid((string)value))
                return ValidationResult.Success;
            else
                return new ValidationResult("Not a correctly formatted Oid");
        }

        public static bool IsValid(string value)
        {
            if (value == null) return true;

            return Regex.IsMatch(value, "^" + Oid.PATTERN + "$", RegexOptions.Singleline);
        }
    }
}

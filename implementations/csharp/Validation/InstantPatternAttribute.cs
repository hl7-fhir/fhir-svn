﻿using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace Hl7.Fhir.Validation
{
    [AttributeUsage(AttributeTargets.Property, Inherited = false, AllowMultiple = false)]
    public class InstantPatternAttribute : ValidationAttribute
    {
        protected override ValidationResult IsValid(object value, ValidationContext validationContext)
        {
            if (validationContext.ObjectType != typeof(DateTimeOffset))
                throw new ArgumentException("IdPatternAttribute can only be applied to DateTimeOffset properties");

            if(value == null) return ValidationResult.Success;

            //TODO: Check FHIR specific rules
            return ValidationResult.Success;
        }
    }
}

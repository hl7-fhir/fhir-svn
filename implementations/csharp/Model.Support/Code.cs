﻿/*
  Copyright (c) 2011-2012, HL7, Inc
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:
  
   * Redistributions of source code must retain the above copyright notice, this 
     list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright notice, 
     this list of conditions and the following disclaimer in the documentation 
     and/or other materials provided with the distribution.
   * Neither the name of HL7 nor the names of its contributors may be used to 
     endorse or promote products derived from this software without specific 
     prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
  POSSIBILITY OF SUCH DAMAGE.
  

*/


using System;
using System.Reflection;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using Hl7.Fhir.Support;

namespace Hl7.Fhir.Model
{
    public partial class Code
    {
        public static bool TryParseValue(string value, out string result)
        {
            if (value == null || Regex.IsMatch(value, "^" + PATTERN + "$", RegexOptions.Singleline ))
            {
                result = value;
                return true;
            }
            else
            {
                result = null;
                return false;
            }
        }

        public static string ParseValue(string value)
        {
            string result = null;

            if (TryParseValue(value, out result))
                return result;
            else
                throw new FhirFormatException("Not a correctly formatted code value");
        }

        internal override ErrorList ValidateRules()
        {
            var result = new ErrorList();

            if (Value == null)
                result.Add("Code values cannot be empty");
            else
            {
                string dummy;

                if (!TryParseValue(Value, out dummy))
                    result.Add("Not a correctly formatted code value");
            }

            return result; 
        }

        public override string ToString()
        {
            return Value;
        }
    }

    [FhirPrimitiveType("codeOfT")]
    public class Code<T> : PrimitiveElement where T : struct
    {
        // Primitive value of element
        public T? Value { get; set; }

        public Code() : this(null) {}

        public Code(T? value)
        {
            if (!typeof(T).IsEnum) 
                throw new ArgumentException("T must be an enumerated type");

            Value = value;
        }

        public static bool TryParseValue(string value, out T result)
        {
            if (value == null) throw new ArgumentNullException("value");

            object res;

            if (EnumHelper.TryParseEnum(value, typeof(T), out res))
            {
                result = (T)res;
                return true;
            }
            else
            {
                result = default(T);
                return false;
            }
        }

        public static T ParseValue(string value)
        {
            T result;

            if (TryParseValue(value, out result))
                return result;
            else
                throw new FhirFormatException("'" + value + "' is not a correct value for " +
                            "enum " + typeof(T).Name );
        }


        public override ErrorList Validate()
        {
            var code = new Code(this.ToString());
            code.Extension = this.Extension;
            code.Id = this.Id;

            return code.Validate();
        }

        public override string ToString()
        {
            if (this.Value.HasValue)
                return EnumHelper.EnumToString(this.Value, typeof(T));
            else
                return null;
        }
    }
}

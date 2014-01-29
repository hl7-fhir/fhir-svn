﻿/*
  Copyright (c) 2011-2013, HL7, Inc.
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
  

 * Generated on Thu, Jan 30, 2014 05:26+1100 for FHIR v0.12
 */
/*
 * null
 */
#import "FHIRValueSetExpansionContainsComponent.h"

#import "FHIRUri.h"
#import "FHIRCode.h"
#import "FHIRString.h"
#import "FHIRValueSetExpansionContainsComponent.h"

#import "FHIRErrorList.h"

@implementation FHIRValueSetExpansionContainsComponent

- (NSString *)system
{
    if(self.systemElement)
    {
        return [self.systemElement value];
    }
    return nil;
}

- (void )setSystem:(NSString *)system
{
    if(system)
    {
        [self setSystemElement:[[FHIRUri alloc] initWithValue:system]];
    }
    else
    {
        [self setSystemElement:nil];
    }
}


- (NSString *)code
{
    if(self.codeElement)
    {
        return [self.codeElement value];
    }
    return nil;
}

- (void )setCode:(NSString *)code
{
    if(code)
    {
        [self setCodeElement:[[FHIRCode alloc] initWithValue:code]];
    }
    else
    {
        [self setCodeElement:nil];
    }
}


- (NSString *)display
{
    if(self.displayElement)
    {
        return [self.displayElement value];
    }
    return nil;
}

- (void )setDisplay:(NSString *)display
{
    if(display)
    {
        [self setDisplayElement:[[FHIRString alloc] initWithValue:display]];
    }
    else
    {
        [self setDisplayElement:nil];
    }
}


- (FHIRErrorList *)validate
{
    FHIRErrorList *result = [[FHIRErrorList alloc] init];
    
    [result addValidation:[super validate]];
    
    if(self.systemElement != nil )
        [result addValidationRange:[self.systemElement validate]];
    if(self.codeElement != nil )
        [result addValidationRange:[self.codeElement validate]];
    if(self.displayElement != nil )
        [result addValidationRange:[self.displayElement validate]];
    if(self.contains != nil )
        for(FHIRValueSetExpansionContainsComponent *elem in self.contains)
            [result addValidationRange:[elem validate]];
    
    return result;
}

@end

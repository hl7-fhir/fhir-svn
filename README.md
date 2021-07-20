## The FHIR Specification Publisher
This library builds and publishes the FHIR specification, based on the contained spreadsheet data in the project.

| CI Status ([master][Link-BuildFhirOrgMaster]) | CI Status ([R4B][Link-BuildFhirOrgR4B]) | 
| :---: | :---: |
| [![Build Status][Badge-AzureMasterPipeline]][Link-AzureMasterPipeline] | [![Build Status][Badge-AzureR4BPipeline]][Link-AzureR4BPipeline] |

### Publishing Locally

1. Run `./gradlew publish` from the command line
2. Wait for it to finish (~20 minutes)

See also: [FHIR Build Process][Link-Wiki]

##### If running commands on the terminal is a frightening prospect for you...

We provide executable script files for Windows (publish.bat) and Mac (publish.sh).

### Command line parameters

There are multiple options available for publishing:

 * `--offline`: use this arg if you are offline and cannot fetch dependencies
 
 * `-nogen`: don't generate the spec, just run the validation. (to use this,
   manually fix things in the publication directory, and then migrate the
changes back to source when done. this is a hack)

 * `-noarchive`: don't generate the archive. Don't use this if you're a core
   editor

 * `-web`: produce the HL7 ready publication form for final upload (only core
   editors)

 * `-diff`: the executable program to use if platform round-tripping doesn't
   produce identical content (default: c:\program files
(x86)\WinMerge\WinMergeU.exe)

 * `-name`: the "name" to go in the title bar of each of the specification

To add any of these options to the publish task, run the command as `./gradlew publish --args"<YOUR ARGS HERE>"`

For example, if you wanted to publish without generating the spec, just running the validation, you would run the command `./gradlew publish --args="-nogen"`

### Publishing Globally

Each time a pull request is opened, the [pull request pipeline][Link-AzurePRPipeline] runs. If the pipeline successfully publishes, it uploads the build as a 
separate branch on [build.fhir.org/branches][Link-BuildFhirOrgBranches], where it can be reviewed to ensure accuracy.

Once merged to master, the [master branch pipeline][Link-AzureMasterPipeline] runs. If successful, the published specification is uploaded to the main 
[build.fhir.org][Link-BuildFhirOrgMaster] webpage.

The only exception to the above is the build for `R4B`. The [R4B pipline][Link-AzureR4BPipeline] detects changes to the [R4B branch][Link-R4BGithub] in github, and 
publishes any changes from that branch to [build.fhir.org/R4B][Link-BuildFhirOrgR4B].

### Maintenance
This project is maintained by [Grahame Grieve][Link-grahameGithub] and [Mark Iantorno][Link-markGithub] on behalf of the FHIR community.

---

[Link-AzureMasterPipeline]: https://dev.azure.com/fhir-pipelines/fhir-publisher/_build/latest?definitionId=44&branchName=refs%2Fpull%2F1084%2Fmerge
[Link-AzureR4BPipeline]: https://dev.azure.com/fhir-pipelines/fhir-publisher/_build/latest?definitionId=46&branchName=R4B
[Link-AzurePRPipeline]: https://dev.azure.com/fhir-pipelines/fhir-publisher/_build/latest?definitionId=42&branchName=refs%2Fpull%2F1084%2Fmerge
[Link-BuildFhirOrgMaster]: https://build.fhir.org
[Link-BuildFhirOrgBranches]: https://build.fhir.org/branches/
[Link-BuildFhirOrgR4B]: https://build.fhir.org/branches/R4B/
[Link-Wiki]: http://wiki.hl7.org/index.php?title=FHIR_Build_Process
[Link-R4BGithub]: https://github.com/HL7/fhir/tree/R4B
[Link-grahameGithub]: https://github.com/grahamegrieve
[Link-markGithub]: https://github.com/markiantorno

[Badge-AzureMasterPipeline]: https://dev.azure.com/fhir-pipelines/fhir-publisher/_apis/build/status/Master%20Branch%20Pipeline?branchName=refs%2Fpull%2F1084%2Fmerge
[Badge-AzureR4BPipeline]: https://dev.azure.com/fhir-pipelines/fhir-publisher/_apis/build/status/R4B%20Pipeline?branchName=R4B

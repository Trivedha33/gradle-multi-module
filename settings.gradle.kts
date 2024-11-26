rootProject.name = "gradle-multi-module"

// Include all subprojects
include("subprojects:app")
include("subprojects:common")
include("build-logic-core")
include("testing")
include("architecture")


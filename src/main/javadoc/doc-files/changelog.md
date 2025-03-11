### 0.4: 3/11/2025
   * Added DELETE of subject/version with support for permanent parameter.
   * Added HTTPD error codes that I mistakenly thought should not be returned.

### 0.3: 2/26/2025
   * Added missing JSON fields to response for check upload "/subjects/..." request.
   * Added an id_ prefix to the version symlinks so they don't create fake infinite link loops.

### 0.2: 2/26/2025
   * Moved SchemaDetails into it's own class.
   * Some refactoring.
   * Moved to deploying a normal jar and a shaded jar.
   * Added the ability to add a prefix if running as part of a larger web service.

### 0.1: 2/25/2025
   * Initial somewhat working version.

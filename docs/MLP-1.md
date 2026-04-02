1. Build the following controllers:
- Review contolller
- Employee Contoller
- Cycle Controller
- Goal Controller

2. Build a working Spring Boot application with the following endpoints:
●	POST /employees — create an employee in employee controller
●	POST /reviews — submit a performance review for an employee in a given cycle in review controller
●	GET /employees/{id}/reviews — get all reviews for an employee with cycle details
* POST /cycle - create a cycle
●	GET /cycles/{id}/summary — return average rating, top performer, and count of completed vs missed goals for a given review cycle
●	GET /employees?department={dept}&minRating={x} — filter employees by department and minimum average rating
* Use JPA/Hibernate for persistence with PostgreSQL.

Constraints:
- do not add anything extra
- do not add tests.
- build only the endpoints mentioned above
- Add Clean structure, validation, error handling, HTTP semantics
- removed any other extra files which are not required
- clean the structures which are not required 
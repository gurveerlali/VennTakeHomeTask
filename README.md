# Transaction Velocity Limiter

A Spring Boot application that processes customer money load requests and enforces velocity limits

Before running, populate the `input.txt` file with your test cases and the program will produce an `output.txt` file 

## Running

```bash
./gradlew bootRun
```

## Testing

```bash
./gradlew test
```

## Design Decisions

- **In-memory H2 database** with JPA for persistence -- schema is auto-generated from entity annotations, with indexes on frequently queried columns. Since this is a small program we don't need to use a larger database design. In a larger scale system we would probably have a separate microservice handling all customer/user data which would be used inside a payment processor service that would process other types of payments too. On top of this we would most likely have a separate table for `Velocity Controls` that could be applied on a per customer/user or perhaps program basis.
- **File-based batch processing** via Spring's `CommandLineRunner` -- input/output paths are configurable via `app.input-file` and `app.output-file` properties. Ideally in a larger scale system this would be a web `spring-boot-web` and we would process each request as a `POST` request handled by a `TransactionController`
- **Per-line error handling** in the file processor -- malformed lines are logged and skipped rather than aborting the batch
- **Input validation** on all request fields before processing, with a custom `InvalidLoadRequestException`
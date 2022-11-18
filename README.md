# AWS IRSA Error Reproducer

IAM Roles for Service Accounts (IRSA) allows EKS pods to assume an IAM role.

[Kpow for Apache Kafka](https://kpow.io) is an enterprise toolkit for Apache Kafka that integrates multiple AWS services including LicenseManager, AWS Glue, and MSK.

Including the AWS Glue dependency silently breaks IRSA, causing the pod to run under the NodeInstanceRole rather than the properly configured IRSA role.

This is likely to impact projects intending to use IRSA with MSK, as the MSK IAM library and AWS Glue libraries are very likely to be included in those projects.

Isolating this error required a full deploy into an IRSA enabled EKS environment with debug logging in place.

## Details

Running Kpow in EKS with IRSA in place and no workaround applied (and debug logging turned on)

```bash
01:30:45.217 ERROR [main] instruct.system – [:instruct.system/init :kafka/primary-cluster] instruction failed
software.amazon.awssdk.services.licensemanager.model.AuthorizationException: User: arn:aws:sts::489728315157:assumed-role/eksctl-awsmp-kpow-example-nodegro-NodeInstanceRole-RF0DW6JPCQ07/i-0dd68413a10f85f5c is not authorized to perform: license-manager:CheckoutLicense because no identity-based policy allows the license-manager:CheckoutLicense action (Service: LicenseManager, Status Code: 400, Request ID: c2546bfe-6a8e-4d0f-a635-36d07ddacad2)
```

This role fallback happens because the WebIdentityTokenCredentialsProvider fails to execute:

(note, this is a DEBUG log line)

```bash
01:30:44.137 DEBUG [main] s.a.a.a.c.AwsCredentialsProviderChain – Unable to load credentials from WebIdentityTokenCredentialsProvider(): Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath
software.amazon.awssdk.core.exception.SdkClientException: Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath
        at software.amazon.awssdk.core.exception.SdkClientException$BuilderImpl.build(SdkClientException.java:102)
        at software.amazon.awssdk.core.internal.http.loader.ClasspathSdkHttpServiceProvider.loadService(ClasspathSdkHttpServiceProvider.java:62)
```

The error is caused by conflicting http implementations brought in by:

LicenseManager or MSK (Iam Auth): ApacheHttpClient
Glue: URLConnection

## Workaround

Manually set the http client implementation:

```clojure
(System/setProperty "software.amazon.awssdk.http.service.impl" "software.amazon.awssdk.http.apache.ApacheSdkHttpService")
```

It is not clear how this setting impacts any of the libraries, but Glue/LM/MSK appear to work with that setting and IRSA roles are resumed.

## Reproduce

This project includes the latest version of three AWS dependencies:

```
[software.amazon.awssdk/licensemanager "2.18.20"]
[software.amazon.msk/aws-msk-iam-auth "1.1.5"]
[software.amazon.glue/schema-registry-serde "1.1.14"]
```

The [irsa-error-repro](src/irsa_error_repro.clj) namespace includes a clojure function instantiates a ClasspathSdkHttpServiceProvider/syncProvider and calls 'loadService'.

Any combination licensemanager or aws-msk-iam-auth libraries error when paired with schema-registry-serde.

Install Leiningen:

```bash
brew install lein
```

Run the reproducer:

```bash
lein run irsa-error-repro
```

See the result:

```bash
Execution error (SdkClientException) at software.amazon.awssdk.core.exception.SdkClientException$BuilderImpl/build (SdkClientException.java:102).
Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath
```

## License

Copyright © 2022 Factor House, open-sourced under the MIT license.

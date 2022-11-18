# AWS IRSA Error Reproducer

IAM Roles for Service Accounts (IRSA) allows EKS pods to assume an IAM role.

[Kpow for Apache Kafka](https://kpow.io) is an enterprise toolkit for Apache Kafka that integrates multiple AWS services including LicenseManager, AWS Glue, and MSK.

Including the AWS Glue dependency silently breaks IRSA, causing the pod to run under the NodeInstanceRole rather than the properly configured IRSA role.

```bash
01:30:45.217 ERROR [main] instruct.system – [:instruct.system/init :kafka/primary-cluster] instruction failed
software.amazon.awssdk.services.licensemanager.model.AuthorizationException: User: arn:aws:sts::489728315157:assumed-role/eksctl-awsmp-kpow-example-nodegro-NodeInstanceRole-RF0DW6JPCQ07/i-0dd68413a10f85f5c is not authorized to perform: license-manager:CheckoutLicense because no identity-based policy allows the license-manager:CheckoutLicense action (Service: LicenseManager, Status Code: 400, Request ID: c2546bfe-6a8e-4d0f-a635-36d07ddacad2)
```

This role fallback happens because the WebIdentityTokenCredentialsProvider fails to execute:

```bash
s.a.a.a.c.AwsCredentialsProviderChain – Unable to load credentials from WebIdentityTokenCredentialsProvider(): Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath
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

## Usage

```bash
brew install lein
```

```bash
lein run irsa-error-repro
```

```bash
Execution error (SdkClientException) at software.amazon.awssdk.core.exception.SdkClientException$BuilderImpl/build (SdkClientException.java:102).
Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath
```

## License

Copyright © 2022 Factor House, open-sourced under the MIT license.
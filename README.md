# Establish identity & trust during chatbot interactions using IBM Security Verify

Chat bots are becoming a popular way to interact with customers. In any business conversation, validation of the customer identity is very essential. Identity verification prevents anyone from carrying out an action on other people's behalf, creating false identities, or committing fraud. IBM Security Verify can be used to establish identity and trust during chatbot interactions.

[IBM Security Verify](https://www.ibm.com/verify/verify-identity) protects users and applications with a cloud-native, software-as-a-service (SaaS) approach that leverages the cloud. Beyond single-sign on and multifactor authentication, Verify is a modernized, modular IDaaS that provides deep AI-powered context for [risk based authentication and adaptive access](https://www.ibm.com/verify/verify-identity/adaptive-access). IBM Security Verify provides a centralized user management interface that can be managed through the admin UI or automatically through REST APIs. It is built upon open standards such as OpenID Connect and SAML 2.0. There are a number of multifactor verification methods available out-of-the-box like SMS, Email, Voice callback one-time passcode, Push notification approval, Knowledge questions and so on. This code pattern uses the single-sign on authentication and email based multifactor verification method.

This code pattern demonstrates a use case in retail domain where customers can create their account by registering to the application and then place order for the items. Later, if they want to check their orders, cancel the order then this facility is provided through chatbot. Chatbot is integrated with the webapp and avoid the hassle of login into the app to check your order. Though user does not need to login to start conversation with chatbot but with the help of security verify user's identity is validated by verifying email-id and the one time code sent by security verify. Once the identity is validated, the chatbot allows user to communicate further regarding their orders.

The code pattern explains the following:
* Add SSO to a java based webapp
* Design chatbot conversation using Watson Assistant
* Implementing IBM Cloud Functions to be used as webhook with chatbot
* Integration of security verify with Watson assistant to establish identity and trust

## Flow

## Pre-requisites

- [IBM Cloud account](https://cloud.ibm.com/)
- [Red Hat OpenShift cluster](https://cloud.ibm.com/kubernetes/catalog/create?platformType=openshift) to deploy webapp.
- [IBM Security Verify Account](https://www.ibm.com/verify/verify-identity) - Trial login can be used for this code pattern.
- [Git client](https://git-scm.com/downloads)
- [The OpenShift CLI (oc)](https://cloud.ibm.com/docs/openshift?topic=openshift-openshift-cli)

## Steps

1. [Clone the repository](#1-clone-the-repository)
2. [Create IBM Cloud Services instances](#2-create-ibm-cloud-services)
3. [Configuration of services](#3-configuration-of-services)
4. [Deploy Application](#4-deploy-application)
5. [Access the Application](#5-access-the-application)

### 1. Clone the repository

From a command terminal, run the below command to clone the repo:
```
git clone https://github.com/IBM/secure-chatbot-interactions-using-security-verify
```

### 2. Create IBM Cloud Services instances

#### 2.1 Sign up for IBM Security Verify

Click [Security Verify](https://www.ibm.com/account/reg/signup?formid=urx-30041) to sign up for Security Verify. After you sign up for an account, the account URL (https://[tenant name].verify.ibm.com/ui/admin) and password is sent in an email.

#### 2.2 Create IBM DB2 Service instance

In [IBM Cloud Catalog](https://cloud.ibm.com/catalog) search of DB2. Click on the Db2 tile.

Select an appropriate plan. Read and accept the license agreement. You may leave the other options to their default values. Click on `Create` button. It will take a minute or so to provision an instance.

**Make a note of service credentials**
- Click the newly created db2 entry in [IBM Cloud Resource list](https://cloud.ibm.com/resources)
- Click `Service credentials` on the left hand side navigation menu. If there are no credentials, then click on `New credential` button and create new credentials. 

![](./images/db2-credentials.png)

- Copy the credentials and paste the contents in the file `<cloned repo parent folder>/sources/data-access-service/src/main/resources/credentials-db2.json`. 
- Make a note of `username` value. This value will be used to create database tables.
- Save the file.

#### 2.3 Create Watson Assistant Service instance

## Summary

## License
This code pattern is licensed under the Apache License, Version 2. Separate third-party code objects invoked within this code pattern are licensed by their respective providers pursuant to their own separate licenses. Contributions are subject to the [Developer Certificate of Origin, Version 1.1](https://developercertificate.org/) and the [Apache License, Version 2](https://www.apache.org/licenses/LICENSE-2.0.txt).

[Apache License FAQ](https://www.apache.org/foundation/license-faq.html#WhatDoesItMEAN)

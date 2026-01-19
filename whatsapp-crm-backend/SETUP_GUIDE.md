# WhatsApp CRM Integration - Complete Setup & Testing Guide

## 📋 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Azure AD & Dynamics 365 Setup](#azure-ad--dynamics-365-setup)
4. [Meta WhatsApp Business API Setup](#meta-whatsapp-business-api-setup)
5. [Spring Boot Application Setup](#spring-boot-application-setup)
6. [n8n Workflow Setup](#n8n-workflow-setup)
7. [Testing Guide](#testing-guide)
8. [API Reference](#api-reference)

---

## 🏗 Architecture Overview

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   WhatsApp      │────▶│  Spring Boot     │────▶│  Dynamics 365   │
│   Business API  │◀────│  Backend         │◀────│  CRM (Dataverse)│
└─────────────────┘     └────────┬─────────┘     └─────────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │    n8n Workflows │
                        │    (Automation)  │
                        └──────────────────┘
```

### Key Components:
- **Spring Boot Backend**: Handles WhatsApp webhooks, chatbot logic, CRM operations
- **Dynamics 365 Dataverse API**: Customer data, leads, quotes, orders, complaints
- **n8n Workflows**: Automation, notifications, CRM event triggers
- **WhatsApp Business API**: Message sending/receiving via Meta Cloud API

---

## ✅ Prerequisites

1. **Java 17+** and **Maven 3.8+**
2. **Microsoft Dynamics 365** instance with Dataverse
3. **Azure AD** tenant with app registration permissions
4. **Meta Developer Account** with WhatsApp Business API access
5. **Self-hosted n8n** with public HTTPS URL

---

## 🔐 Azure AD & Dynamics 365 Setup

### Step 1: Register Azure AD Application

1. Go to [Azure Portal](https://portal.azure.com) → **Azure Active Directory** → **App registrations**
2. Click **New registration**:
   - Name: `WhatsApp CRM Integration`
   - Supported account types: `Single tenant`
   - Redirect URI: Leave empty (we use client credentials)
3. Note down:
   - **Application (client) ID**
   - **Directory (tenant) ID**

### Step 2: Create Client Secret

1. In your app → **Certificates & secrets** → **New client secret**
2. Description: `WhatsApp Integration Secret`
3. Expiry: Choose appropriate duration
4. **Copy the secret value immediately** (shown only once)

### Step 3: Grant API Permissions

1. Go to **API permissions** → **Add a permission**
2. Select **Dynamics CRM** → **Delegated permissions**
3. Add: `user_impersonation`
4. Click **Grant admin consent**

### Step 4: Add Application User in Dynamics 365

1. Go to your Dynamics 365 → **Settings** → **Security** → **Users**
2. Switch to **Application Users** view
3. Create new application user:
   - Application ID: (paste your Azure AD client ID)
   - Assign **System Administrator** or custom security role

### Step 5: Find Your Dynamics 365 URL

Your Dataverse Web API URL format:
```
https://YOUR-ORG.crm.dynamics.com/api/data/v9.2
```

---

## 📱 Meta WhatsApp Business API Setup

### Step 1: Create Meta App

1. Go to [Meta Developer Portal](https://developers.facebook.com/)
2. **Create App** → Select **Business** type
3. Add **WhatsApp** product to your app

### Step 2: Configure WhatsApp

1. In WhatsApp product → **Getting Started**
2. Note down:
   - **Phone Number ID**
   - **WhatsApp Business Account ID**
   - **Temporary Access Token** (for testing)

### Step 3: Create Permanent Access Token

1. Go to **Business Settings** → **System Users**
2. Create system user with **Admin** role
3. Add your WhatsApp app as an asset
4. Generate permanent token with permissions:
   - `whatsapp_business_messaging`
   - `whatsapp_business_management`

### Step 4: Configure Webhook

1. In WhatsApp → **Configuration** → **Webhook**
2. Callback URL: `https://YOUR-BACKEND-URL/api/webhook/whatsapp`
3. Verify Token: Create a secure random string
4. Subscribe to: `messages`, `message_template_status_update`

---

## ⚙️ Spring Boot Application Setup

### Step 1: Configure application.yml

```yaml
dynamics365:
  base-url: https://YOUR-ORG.crm.dynamics.com
  api-version: v9.2
  azure:
    tenant-id: YOUR-AZURE-TENANT-ID
    client-id: YOUR-AZURE-CLIENT-ID
    client-secret: YOUR-AZURE-CLIENT-SECRET
    scope: https://YOUR-ORG.crm.dynamics.com/.default

whatsapp:
  api:
    base-url: https://graph.facebook.com/v18.0
    phone-number-id: YOUR-PHONE-NUMBER-ID
    access-token: YOUR-PERMANENT-ACCESS-TOKEN
    verify-token: YOUR-WEBHOOK-VERIFY-TOKEN

n8n:
  base-url: https://YOUR-N8N-DOMAIN.com
  webhooks:
    incoming-message: /webhook/whatsapp-incoming
    quotation-response: /webhook/quotation-response
    lead-created: /webhook/lead-created
    complaint-registered: /webhook/complaint-registered
    do-request: /webhook/do-request
```

### Step 2: Build & Run

```bash
cd whatsapp-crm-backend
mvn clean install
mvn spring-boot:run
```

### Step 3: Verify Deployment

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/crm
```

---

## 🔄 n8n Workflow Setup

### Import these workflows in n8n:

#### 1. Incoming Message Handler (`/webhook/whatsapp-incoming`)
```
Webhook → IF (event type) → Route to appropriate handler
```

#### 2. CRM Event Trigger - Quote Created
```
Schedule/CRM Webhook → Get Quote Details → HTTP POST to /api/notifications/quotation
```

#### 3. CRM Event Trigger - Sales Order Created
```
CRM Webhook → HTTP POST to /api/notifications/sales-order-created
```

#### 4. Daily Birthday Greetings
```
Cron (daily 9AM) → Query CRM for today's birthdays → Loop → Send greeting
```

---

## 🧪 Testing Guide

### Test 1: Verify WhatsApp Webhook

```bash
# Webhook verification
curl "http://localhost:8080/api/webhook/whatsapp?hub.mode=subscribe&hub.verify_token=YOUR_TOKEN&hub.challenge=test123"
# Expected: test123
```

### Test 2: Test CRM Connection

```bash
curl http://localhost:8080/api/health/crm
# Expected: {"status":"UP","message":"Successfully connected to Dynamics 365"}
```

### Test 3: Query Customers

```bash
curl http://localhost:8080/api/crm/customers
```

### Test 4: Create a Lead

```bash
curl -X POST http://localhost:8080/api/crm/leads \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "Customer",
    "companyName": "Test Corp",
    "email": "test@example.com",
    "phone": "919876543210",
    "subject": "WhatsApp Test Lead"
  }'
```

### Test 5: Send Test WhatsApp Message

```bash
curl -X POST http://localhost:8080/api/notifications/customer-onboarded \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "919876543210",
    "customerName": "Test Customer",
    "salesPersonName": "Sales Rep"
  }'
```

---

## 📚 API Reference

### CRM Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/crm/customers` | Get all customers |
| GET | `/crm/customers/by-phone/{phone}` | Find customer by phone |
| POST | `/crm/leads` | Create new lead |
| POST | `/crm/opportunities` | Create opportunity |
| POST | `/crm/complaints` | Register complaint |
| POST | `/crm/delivery-orders` | Create delivery order |
| GET | `/crm/entities/custom` | List custom entities |

### Notification Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/notifications/quotation` | Send quote with buttons |
| POST | `/notifications/sales-order-created` | SO confirmation |
| POST | `/notifications/complaint-registered` | Complaint ticket |
| POST | `/notifications/birthday` | Birthday greeting |

---

## 🔧 Custom Entity Configuration

If you have custom tables in Dataverse:

1. Get entity metadata:
```bash
curl "http://localhost:8080/api/crm/entities/cr_deliveryorders/metadata"
```

2. Update entity names in `application.yml`:
```yaml
crm:
  entities:
    complaint: cr_complaints
    delivery-order: cr_deliveryorders
```

---

## 📞 Support

For issues:
1. Check `/api/health/crm` for CRM connectivity
2. Review Spring Boot logs for detailed errors
3. Test individual API endpoints using Swagger UI at `/swagger-ui.html`

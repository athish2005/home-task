# Pastebin-Lite

A lightweight Pastebin-style application that allows users to create and share text snippets with optional **expiration time** and **maximum view limits**.

The project consists of a **Spring Boot backend** and a **React (Vite) frontend**, both deployed separately.

---

## 🚀 Live Demo

- **Frontend:** https://exquisite-gumdrop-5c3a10.netlify.app/

---

## 🛠 Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Maven

### Frontend
- React
- Vite
- Axios
- Tailwind CSS
- React Router

---

## ✨ Features

- Create a paste with text content
- Optional **time-to-live (TTL)** in seconds
- Optional **maximum view count**
- Secure shareable URL
- Paste automatically expires when:
  - TTL is exceeded, or
  - View limit is reached
- Remaining views displayed when viewing a paste
- RESTful API with proper error handling


# GX Frontend
> Updated on: 2026/06/16

Modern React-based frontend application with TypeScript, featuring responsive design and efficient state management.

---

## 🚀 Features

- **Modern React** - Built with React hooks and functional components
- **TypeScript** - Full type safety and better developer experience
- **State Management** - Context API / Redux / Zustand for predictable state handling
- **Responsive Design** - Mobile-first approach with CSS modules / Tailwind / Material UI
- **API Integration** - Seamless integration with backend services
- **Component-Driven** - Reusable, well-organized UI components

---

## 📦 Installation

### Prerequisites
- Node.js (v16 or higher)
- npm or yarn

### Setup
Go to the frontend directory and install dependencies:
```bash
cd frontend
npm install
```

### Development
Start the local development server:
```bash
npm run dev
```
By default, Vite runs on:
```
http://localhost:3000
```

### Build
Build the production bundle:
```bash
npm run build
```
---

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run type-check` - Run TypeScript type checking

---
## Root Files and Folders

`docs/`: Text files and documentation related to the frontend application. (e.g., API contracts, design guidelines, etc.)
`public/`: Static assets like images, icons, and fonts.
`src/`: Source code for the frontend application, including components, pages, styles, and utilities
`components.json`: shadcn/ui-style configuration for aliases, Tailwind, and icons.
`eslint.config.js`: ESLint configuration for code quality and consistency.
`index.html`: Vite HTML entrypoint.
`package-lock.json`: npm package lock file for dependency management.
`package.json`: npm package configuration with scripts and dependencies.
`postcss.config.js`: PostCSS configuration for Tailwind CSS and autoprefixer.
`tailwind.config.js`: Tailwind CSS configuration for custom styles and themes.
`tsconfig.app.json`: TypeScript configuration for the frontend app.
`tsconfig.json`: Root TypeScript configuration.
`tsconfig.node.json`: TypeScript configuration for Node.js environment.
`vite-env.d.ts`: TypeScript declaration file for Vite environment.
`vite.config.ts`: Vite configuration for development and production builds.

---

## License

This project is part of the GX platform.
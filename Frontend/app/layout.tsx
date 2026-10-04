import type { Metadata } from "next";
import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import { AuthProvider } from "../hooks/useAuth";
import "./globals.css";

export const metadata: Metadata = {
  title: "EnterpriseFlow",
  description: "Enterprise productivity and company management platform.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className="flex min-h-screen flex-col">
        <AuthProvider>
          <Navbar />
          <div className="app-content">{children}</div>
          <Footer />
        </AuthProvider>
      </body>
    </html>
  );
}

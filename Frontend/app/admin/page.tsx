import AdminDashboard from "../../components/AdminDashboard";
import AdminGuard from "../../components/AdminGuard";
import ProtectedRoute from "../../components/ProtectedRoute";

export default function AdminPage() { return <ProtectedRoute><AdminGuard><AdminDashboard /></AdminGuard></ProtectedRoute>; }

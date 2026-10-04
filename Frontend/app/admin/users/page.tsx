import AdminGuard from "../../../components/AdminGuard";
import ProtectedRoute from "../../../components/ProtectedRoute";
import UsersManager from "../../../components/UsersManager";

export default function AdminUsersPage() { return <ProtectedRoute><AdminGuard><UsersManager /></AdminGuard></ProtectedRoute>; }

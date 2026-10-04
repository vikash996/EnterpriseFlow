import AdminGuard from "../../components/AdminGuard";
import CommandCenterManager from "../../components/CommandCenterManager";
import ProtectedRoute from "../../components/ProtectedRoute";

export default function CommandCenterPage() {
  return <ProtectedRoute><AdminGuard><CommandCenterManager /></AdminGuard></ProtectedRoute>;
}

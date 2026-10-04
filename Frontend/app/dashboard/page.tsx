import ProtectedRoute from "../../components/ProtectedRoute";
import WorkspacePage from "../../components/WorkspacePage";

export default function DashboardPage() {
  return <ProtectedRoute><WorkspacePage kind="dashboard" /></ProtectedRoute>;
}

import ProtectedRoute from "../../components/ProtectedRoute";
import ProjectManager from "../../components/ProjectManager";

export default function TasksPage() {
  return <ProtectedRoute><ProjectManager tasksOnly /></ProtectedRoute>;
}

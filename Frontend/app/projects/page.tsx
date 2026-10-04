import ProtectedRoute from "../../components/ProtectedRoute";
import ProjectManager from "../../components/ProjectManager";

export default function ProjectsPage() {
  return <ProtectedRoute><ProjectManager /></ProtectedRoute>;
}

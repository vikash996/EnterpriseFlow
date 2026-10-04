import ProtectedRoute from "../../components/ProtectedRoute";
import KnowledgeChat from "../../components/KnowledgeChat";

export default function AiAssistancePage() {
  return <ProtectedRoute><KnowledgeChat /></ProtectedRoute>;
}

type UnavailableRouteProps = {
  title: string;
};

export default function UnavailableRoute({ title }: UnavailableRouteProps) {
  return (
    <main className="mx-auto flex min-h-screen max-w-4xl items-center px-6 py-16">
      <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
    </main>
  );
}
